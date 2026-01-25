package com.barofarm.ai.datagen.application;

import com.barofarm.ai.datagen.application.dto.ProductGenerationDto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

// SQL 파일에서 상품 데이터를 읽어오는 서비스
@Slf4j
@Service
public class SqlDataReaderService {

    // SQL 파일에서 상품 데이터를 파싱하여 ProductGenerationDto 리스트로 반환
    public List<ProductGenerationDto> readProductsFromSql(String sqlFilePath) throws IOException {
        log.info("SQL 파일에서 상품 데이터 읽기 시작: {}", sqlFilePath);

        Path path = Paths.get(sqlFilePath);
        if (!Files.exists(path)) {
            // 클래스패스에서 찾기 시도
            try {
                ClassPathResource resource = new ClassPathResource(sqlFilePath);
                path = resource.getFile().toPath();
            } catch (Exception e) {
                throw new IOException("SQL 파일을 찾을 수 없습니다: " + sqlFilePath, e);
            }
        }

        String content = Files.readString(path);
        List<ProductGenerationDto> products = parseSqlContent(content);

        log.info("SQL 파일 파싱 완료: {}개 상품 데이터 추출", products.size());
        return products;
    }

    private List<ProductGenerationDto> parseSqlContent(String content) {
        List<ProductGenerationDto> products = new ArrayList<>();

        // 'product_dummy_origin.sql' 파일의 INSERT 문을 파싱하기 위한 정규식
        // 형식: INSERT INTO `product` (...) VALUES (uuid_to_bin('id'), uuid_to_bin('sellerId'),
        // '상품명', '설명', uuid_to_bin('카테고리UUID'), 가격, '상태', 'created_at', 'updated_at');
        String regex = "VALUES\\s*\\(uuid_to_bin\\('[^']+'\\),\\s*uuid_to_bin\\('[^']+'\\),\\s*"
            + "'([^']*)',\\s*'([^']*)',\\s*"
            + "uuid_to_bin\\('([^']+)'\\),\\s*(\\d+),\\s*'([^']*)',\\s*'[^']*',\\s*'[^']*'\\)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String productName = matcher.group(1);
            String description = matcher.group(2);
            String categoryUuid = matcher.group(3);
            long price = Long.parseLong(matcher.group(4));
            String status = matcher.group(5);

            // 유효한 카테고리만 포함
            if (isValidCategory(categoryUuid)) {
                products.add(new ProductGenerationDto(
                    productName,
                    description,
                    categoryUuid,
                    price,
                    status
                ));
            }
        }

        return products;
    }

    private boolean isValidCategory(String categoryUuid) {
        // UUID 형식 검증 (기본적인 UUID 패턴 확인)
        if (categoryUuid == null || categoryUuid.trim().isEmpty()) {
            return false;
        }

        // UUID 패턴 검증 (8-4-4-4-12 형식)
        String uuidPattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        return categoryUuid.matches(uuidPattern);
    }
}
