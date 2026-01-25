package com.barofarm.ai.datagen.application;

import com.barofarm.ai.datagen.application.constants.DataGenConstants;
import com.barofarm.ai.datagen.application.dto.AutoAmplifyResponse;
import com.barofarm.ai.datagen.application.dto.ProductGenerationDto;
import com.barofarm.ai.datagen.application.prompt.ProductGenerationPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGenerationService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final SqlDataReaderService sqlDataReaderService;
    private final Random random = new Random();

    public List<ProductGenerationDto> generateProducts(List<ProductGenerationDto> examples, String targetCategory) {
        final String examplesJson = serializeExamplesToJson(examples);
        final String promptTemplate = ProductGenerationPrompt.getPromptTemplate();
        final String jsonFormat = ProductGenerationPrompt.getJsonFormat();

        return chatClient.prompt()
            .user(p -> p.text(promptTemplate)
                .param("examples", examplesJson)
                .param("category", targetCategory)
                .param("format", jsonFormat)
            )
            .call()
            .entity(new ParameterizedTypeReference<List<ProductGenerationDto>>() {});
    }

    // SQL 파일을 기반으로 자동으로 상품 데이터를 증폭
    public AutoAmplifyResponse autoAmplifyProducts(String sqlFilePath) {
        try {
            final int targetCount = DataGenConstants.ProductGeneration.TARGET_COUNT;
            log.info("자동 데이터 증폭 시작: 목표 {}개", targetCount);

            // 1. 시드 데이터 읽기
            List<ProductGenerationDto> seedProducts = sqlDataReaderService.readProductsFromSql(sqlFilePath);

            if (seedProducts.isEmpty()) {
                log.error("시드 데이터가 없습니다: {}", sqlFilePath);
                throw new IllegalArgumentException("시드 데이터가 없습니다: " + sqlFilePath);
            }

            log.info("시드 데이터 로드 완료: {}개 상품", seedProducts.size());

            // 2. LLM으로 데이터 증폭
            List<ProductGenerationDto> amplifiedProducts = generateAmplifiedProducts(seedProducts, targetCount);

            log.info("데이터 증폭 완료: {}개 상품 생성", amplifiedProducts.size());

            // 3. 결과를 CSV 파일로 저장
            if (amplifiedProducts.isEmpty()) {
                log.warn("⚠️ 생성된 상품이 없어 CSV 파일을 생성하지 않습니다.");
            } else {
                saveProductsToCsvFile(amplifiedProducts);
            }

            return new AutoAmplifyResponse(
                seedProducts.size(),
                targetCount,
                amplifiedProducts.size(),
                amplifiedProducts
            );

        } catch (IOException e) {
            log.error("SQL 파일 읽기 실패: {}", sqlFilePath, e);
            throw new RuntimeException("SQL 파일 읽기 실패: " + sqlFilePath, e);
        } catch (Exception e) {
            log.error("데이터 증폭 중 오류 발생", e);
            throw new RuntimeException("데이터 증폭 중 오류 발생", e);
        }
    }

    private List<ProductGenerationDto> generateAmplifiedProducts(
        List<ProductGenerationDto> seedProducts, int targetCount) {
        List<ProductGenerationDto> allNewProducts = new ArrayList<>();
        Set<String> generatedNames = new HashSet<>();

        // 시드 데이터에서 카테고리 추출
        List<String> categories = extractValidCategories(seedProducts);
        log.info("데이터 증폭 시작 - 목표: {}, 카테고리: {}", targetCount, categories);

        int iteration = 0;
        int consecutiveFailures = 0;
        final int maxConsecutiveFailures = DataGenConstants.ProductGeneration.MAX_CONSECUTIVE_FAILURES;
        final int maxIterations = DataGenConstants.ProductGeneration.MAX_ITERATIONS;

        while (allNewProducts.size() < targetCount && iteration < maxIterations) {
            iteration++;

            // 랜덤하게 예시 선택
            List<ProductGenerationDto> examples = selectRandomExamples(
                seedProducts, DataGenConstants.ProductGeneration.MAX_EXAMPLES_COUNT);
            String targetCategoryCode = categories.get(random.nextInt(categories.size()));
            String targetCategoryUuid = DataGenConstants.CATEGORY_CODE_TO_UUID.get(targetCategoryCode);

            log.debug("[{}] 카테고리: {} ({}) | 현재: {}/{}",
                iteration, targetCategoryCode, targetCategoryUuid, allNewProducts.size(), targetCount);

            try {
                List<ProductGenerationDto> newItems = generateProducts(examples, targetCategoryUuid);

                if (newItems == null || newItems.isEmpty()) {
                    log.warn("LLM API가 빈 결과를 반환했습니다 (반복 {})", iteration);
                    consecutiveFailures++;
                    continue;
                }

                // 생성된 상품 검증 및 추가
                int addedCount = addValidProducts(newItems, allNewProducts, generatedNames);

                if (addedCount == 0) {
                    log.warn("생성된 상품 중 유효한 상품이 없습니다 (반복 {})", iteration);
                    consecutiveFailures++;
                } else {
                    consecutiveFailures = 0; // 성공했으므로 실패 카운트 리셋
                }

                logProgress(allNewProducts.size(), targetCount);

            } catch (Exception e) {
                log.error("LLM API 호출 실패 (반복 {}): {}", iteration, e.getMessage());
                consecutiveFailures++;

                if (consecutiveFailures >= maxConsecutiveFailures) {
                    log.error("연속 {}회 실패로 증폭을 중단합니다", maxConsecutiveFailures);
                    break;
                }
            }
        }

        logCompletionStatus(iteration, maxIterations, allNewProducts.size(), targetCount);
        return allNewProducts.subList(0, Math.min(allNewProducts.size(), targetCount));
    }

    private List<String> extractValidCategories(List<ProductGenerationDto> seedProducts) {
        List<String> categories = seedProducts.stream()
            .map(ProductGenerationDto::categoryUuid)
            .map(this::uuidToCategoryCode)
            .distinct()
            .filter(this::isValidCategory)
            .toList();

        if (categories.isEmpty()) {
            log.warn("유효한 시드 카테고리가 없습니다. 기본 카테고리를 사용합니다.");
            return List.of("FRUIT", "VEGETABLE", "ETC");
        }
        return categories;
    }

    private int addValidProducts(
        List<ProductGenerationDto> newItems,
        List<ProductGenerationDto> allNewProducts,
        Set<String> generatedNames) {
        int addedCount = 0;
        for (ProductGenerationDto item : newItems) {
            ProductGenerationDto productWithDefaults = ensureDefaultValues(item);

            if (isValidProduct(productWithDefaults)
                && !generatedNames.contains(productWithDefaults.productName())) {
                allNewProducts.add(productWithDefaults);
                generatedNames.add(productWithDefaults.productName());
                addedCount++;
            }
        }
        return addedCount;
    }

    private void logProgress(int currentSize, int targetCount) {
        if (currentSize % 10 == 0 || currentSize >= targetCount) {
            double progress = (double) currentSize / targetCount * 100;
            log.info("진행 상황: {}/{} ({:.1f}%)", currentSize, targetCount, progress);
        }
    }

    private void logCompletionStatus(int iteration, int maxIterations, int actualSize, int targetCount) {
        if (iteration >= maxIterations) {
            log.warn("최대 반복 횟수({})에 도달했습니다. 생성된 상품: {}개", maxIterations, actualSize);
        }

        if (actualSize < targetCount) {
            log.warn("목표 수량에 도달하지 못했습니다. 목표: {}, 실제: {}", targetCount, actualSize);
        } else {
            log.info("목표 수량 달성: {}개 상품 생성 완료", actualSize);
        }
    }

    private List<ProductGenerationDto> selectRandomExamples(List<ProductGenerationDto> seedProducts, int count) {
        List<ProductGenerationDto> examples = new ArrayList<>(seedProducts);
        int actualCount = Math.min(count, examples.size());

        List<ProductGenerationDto> selected = new ArrayList<>();
        for (int i = 0; i < actualCount; i++) {
            int randomIndex = random.nextInt(examples.size());
            selected.add(examples.get(randomIndex));
            examples.remove(randomIndex);
        }

        return selected;
    }

    private boolean isValidProduct(ProductGenerationDto product) {
        if (product == null) {
            return false;
        }

        // 상품명 검증
        String productName = product.productName();
        if (productName == null || productName.trim().isEmpty()
            || productName.length() > DataGenConstants.ProductGeneration.MAX_PRODUCT_NAME_LENGTH) {
            return false;
        }

        // 설명 검증
        String description = product.description();
        if (description == null || description.trim().isEmpty()
            || description.length() > DataGenConstants.ProductGeneration.MAX_DESCRIPTION_LENGTH) {
            return false;
        }

        // 카테고리 검증
        String categoryCode = uuidToCategoryCode(product.categoryUuid());
        if (categoryCode == null || !isValidCategory(categoryCode)) {
            return false;
        }

        // 가격 검증
        Long price = product.price();
        if (price == null || price < DataGenConstants.ProductGeneration.MIN_PRICE
            || price > DataGenConstants.ProductGeneration.MAX_PRICE) {
            return false;
        }

        // 상품명에 부적절한 키워드가 없는지 확인
        return !containsInappropriateContent(productName)
            && !containsInappropriateContent(description);
    }

    private boolean containsInappropriateContent(String text) {
        if (text == null) {
            return false;
        }

        String lowerText = text.toLowerCase();
        for (String keyword : DataGenConstants.INAPPROPRIATE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidCategory(String category) {
        for (String validCategory : DataGenConstants.VALID_CATEGORIES) {
            if (validCategory.equals(category)) {
                return true;
            }
        }
        return false;
    }

    private String uuidToCategoryCode(String categoryUuid) {
        if (categoryUuid == null) {
            return null;
        }
        return DataGenConstants.CATEGORY_UUID_TO_CODE.getOrDefault(categoryUuid, "UNKNOWN");
    }

    private String serializeExamplesToJson(List<ProductGenerationDto> examples) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(examples);
        } catch (JsonProcessingException e) {
            log.error("Error serializing examples to JSON", e);
            return "[]";
        }
    }

    private void saveProductsToCsvFile(List<ProductGenerationDto> products) {
        try {
            if (products == null || products.isEmpty()) {
                log.warn("⚠️ 저장할 상품 데이터가 없습니다.");
                return;
            }

            Path outputDir = Paths.get(System.getProperty("user.dir"), DataGenConstants.FilePaths.OUTPUT_DIR);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                log.info("📁 디렉토리 생성: {}", outputDir.toAbsolutePath());
            }
            Path outputFile = outputDir.resolve(DataGenConstants.FilePaths.OUTPUT_CSV_FILE);

            List<String> csvLines = new ArrayList<>();
            csvLines.add(DataGenConstants.Csv.CSV_HEADER);

            for (ProductGenerationDto product : products) {
                ProductGenerationDto productWithDefaults = ensureDefaultValues(product);
                String csvLine = formatProductAsCsvLine(productWithDefaults);
                csvLines.add(csvLine);
            }

            Files.writeString(outputFile, String.join("\n", csvLines));
            log.info("✅ CSV 결과 파일 저장 완료: {} ({}개 상품)", outputFile.toAbsolutePath(), products.size());

        } catch (IOException e) {
            log.error("❌ CSV 결과 파일 저장 실패", e);
            throw new RuntimeException("Failed to save CSV file", e);
        } catch (Exception e) {
            log.error("❌ CSV 파일 저장 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("Failed to save CSV file", e);
        }
    }

    private String formatProductAsCsvLine(ProductGenerationDto product) {
        String productName = escapeCsvField(product.productName());
        String description = escapeCsvField(product.description());
        String categoryId = product.categoryUuid();  // UUID를 직접 사용
        long price = product.price();
        String status = product.status() != null && !product.status().isEmpty()
            ? product.status() : DataGenConstants.ProductGeneration.DEFAULT_STATUS;

        return String.format("%s,%s,%s,%d,%s",
            productName, description, categoryId, price, status);
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // CSV에서 쉼표, 따옴표, 줄바꿈이 포함된 경우 처리
        if (field.contains(DataGenConstants.Csv.CSV_DELIMITER)
            || field.contains(DataGenConstants.Csv.CSV_QUOTE)
            || field.contains("\n")) {
            // 따옴표를 두 개로 이스케이프하고 전체를 따옴표로 감싸기
            return DataGenConstants.Csv.CSV_QUOTE
                + field.replace(DataGenConstants.Csv.CSV_QUOTE, DataGenConstants.Csv.CSV_ESCAPED_QUOTE)
                + DataGenConstants.Csv.CSV_QUOTE;
        }
        return field;
    }

    // ProductGenerationDto에 status 기본값이 없으면 설정
    private ProductGenerationDto ensureDefaultValues(ProductGenerationDto product) {
        if (product == null) {
            return null;
        }

        // status가 null이거나 비어있으면 기본값 설정
        if (product.status() == null || product.status().isEmpty()) {
            return new ProductGenerationDto(
                product.productName(),
                product.description(),
                product.categoryUuid(),
                product.price(),
                DataGenConstants.ProductGeneration.DEFAULT_STATUS
            );
        }

        return product;
    }
}
