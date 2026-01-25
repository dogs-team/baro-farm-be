package com.barofarm.ai.recommend.application;

import com.barofarm.ai.embedding.domain.UserProfileEmbeddingDocument;
import com.barofarm.ai.embedding.infrastructure.elasticsearch.UserProfileEmbeddingRepository;
import com.barofarm.ai.recommend.application.dto.ProductRecommendResponse;
import com.barofarm.ai.search.application.VectorProductSearchService;
import com.barofarm.ai.search.domain.ProductDocument;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizedRecommendService {

    private final UserProfileEmbeddingRepository userProfileEmbeddingRepository;
    private final VectorProductSearchService vectorProductSearchService;
    private final MedoidDiversityService medoidDiversityService;

    // 사용자 프로필 벡터를 기반으로 개인화된 상품을 추천
    public List<ProductRecommendResponse> recommendProducts(UUID userId, int topK) {
        // 1. 사용자 프로필 벡터 조회
        UserProfileEmbeddingDocument profile =
            userProfileEmbeddingRepository.findById(userId)
                .orElse(null);

        if (profile == null || profile.getUserProfileVector() == null) {
            log.warn("사용자 ID {}의 프로필 벡터가 없습니다. 임베딩을 먼저 생성해야 합니다.", userId);
            return List.of();
        }

        // 이미 구매했거나 장바구니에 담은 상품은 추천하지 않기 위해 sourceProductIds 사용
        List<String> experiencedProductIds = profile.getSourceProductIds() != null
            ? profile.getSourceProductIds()
            : List.of();  // null이면 빈 리스트 사용

        log.debug("사용자 {}의 추천에서 임베딩의 sourceProductIds({}개)로 상품 제외",
                 userId, experiencedProductIds.size());

        // 3. List<Double>을 float[]로 변환
        // 벡터 데이터가 유효하지 않은 경우 빈 리스트 반환 (예외를 던지지 않음)
        List<Double> userProfileVector = profile.getUserProfileVector();
        if (userProfileVector == null || userProfileVector.isEmpty()) {
            log.warn("사용자 ID {}의 벡터 데이터가 null이거나 비어있습니다. 빈 리스트를 반환합니다.", userId);
            return List.of();
        }

        float[] userVector = convertToFloatArray(userProfileVector);

        // 4. 사용자 선호 카테고리 코드 가져오기
        String preferredCategoryCode = profile.getPreferredCategoryCode();

        // 5. Elasticsearch 벡터 유사도 검색 + 메도이드 다양성 적용
        return findSimilarProductsByVector(userVector, topK, experiencedProductIds, preferredCategoryCode);
    }

    // Elasticsearch에서 벡터 유사도 검색을 수행하고,
    // 메도이드 알고리즘을 적용해 다양성을 확보한 최종 추천 결과를 생성합니다.
    private List<ProductRecommendResponse> findSimilarProductsByVector(
        float[] userVector,
        int topK,
        List<String> experiencedProductIds,
        String preferredCategoryCode
    ) {
        // String을 UUID로 변환
        List<UUID> excludeProductIds = experiencedProductIds.stream()
            .map(UUID::fromString)
            .toList();

        // 같은 카테고리 보너스 점수 설정 (0.3 = 30% 보너스, SimilarProductRecommendService와 동일)
        Double categoryMatchBonus = preferredCategoryCode != null ? 0.3 : null;

        // 메도이드 적용을 위해 topK보다 넉넉한 후보를 가져온다.
        int candidateSize = Math.min(topK * 3, 100);

        List<ProductDocument> candidates = vectorProductSearchService.findSimilarProductDocumentsByVector(
            userVector,
            candidateSize,
            excludeProductIds,   // 제외할 상품 ID들 (이미 경험한 상품들)
            preferredCategoryCode, // 사용자 선호 카테고리 코드
            categoryMatchBonus   // 카테고리 일치 보너스
        );

        if (candidates.isEmpty()) {
            return List.of();
        }

        // 메도이드 알고리즘으로 다양성을 확보한 대표 상품 선택
        List<ProductDocument> medoids =
            medoidDiversityService.selectDiverseMedoids(userVector, candidates, topK);

        // 최종 추천 응답 DTO로 변환
        List<ProductRecommendResponse> recommendations = medoids.stream()
            .map(product -> new ProductRecommendResponse(
                product.getProductId(),
                product.getProductName(),
                product.getProductCategoryName(),
                product.getPrice()
            ))
            .toList();

        // 추천 결과와 근거를 로그로 출력
        logRecommendationResults(recommendations, medoids, candidates, preferredCategoryCode, categoryMatchBonus);

        return recommendations;
    }

    /**
     * 추천 결과와 근거를 로그로 출력합니다.
     */
    private void logRecommendationResults(
        List<ProductRecommendResponse> recommendations,
        List<ProductDocument> medoids,
        List<ProductDocument> candidates,
        String preferredCategoryCode,
        Double categoryMatchBonus
    ) {
        if (recommendations.isEmpty()) {
            log.info("🎯 [추천 결과] 추천된 상품이 없습니다.");
            return;
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("🎯 [추천 결과] 총 %d개 상품 추천 완료\n", recommendations.size()));
        logMessage.append(String.format("   - 후보 상품 수: %d개\n", candidates.size()));
        String categoryDisplay = preferredCategoryCode != null ? preferredCategoryCode : "없음";
        logMessage.append(String.format("   - 선호 카테고리: %s\n", categoryDisplay));
        String bonusDisplay = categoryMatchBonus != null
            ? String.format("%.1f%%", categoryMatchBonus * 100)
            : "없음";
        logMessage.append(String.format("   - 카테고리 보너스: %s\n", bonusDisplay));
        logMessage.append("   - 추천 상품 목록:\n");

        for (int i = 0; i < recommendations.size(); i++) {
            ProductRecommendResponse rec = recommendations.get(i);
            ProductDocument medoid = medoids.get(i);

            // 카테고리 보너스 적용 여부 확인
            boolean hasCategoryBonus = preferredCategoryCode != null
                && medoid.getProductCategoryCode() != null
                && medoid.getProductCategoryCode().equals(preferredCategoryCode);

            logMessage.append(String.format(
                "     %d. [%s] %s (카테고리: %s, 가격: %,d원%s)\n",
                i + 1,
                rec.productId(),
                rec.productName(),
                rec.productCategoryName(),
                rec.price(),
                hasCategoryBonus ? ", 카테고리 보너스 적용" : ""
            ));
        }

        // INFO 레벨로 출력 (DEBUG 레벨도 함께 출력)
        log.info(logMessage.toString());
        log.debug("추천 결과 상세:\n{}", logMessage.toString());
    }

    // List<Double>을 float[]로 변환합니다.
    // 호출 전에 null/empty 체크를 이미 했으므로 여기서는 변환만 수행합니다.
    private float[] convertToFloatArray(List<Double> doubleList) {
        float[] floatArray = new float[doubleList.size()];
        for (int i = 0; i < doubleList.size(); i++) {
            floatArray[i] = doubleList.get(i).floatValue();
        }
        return floatArray;
    }

}
