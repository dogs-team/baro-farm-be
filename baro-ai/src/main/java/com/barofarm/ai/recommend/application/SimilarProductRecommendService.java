package com.barofarm.ai.recommend.application;

import com.barofarm.ai.recommend.application.dto.ProductRecommendResponse;
import com.barofarm.ai.search.application.VectorProductSearchService;
import com.barofarm.ai.search.domain.ProductDocument;
import com.barofarm.ai.search.infrastructure.elasticsearch.ProductSearchRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// 상품 디테일 페이지에서 비슷한 상품 반환 (벡터 유사도)
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarProductRecommendService {

    private final ProductSearchRepository productSearchRepository;
    private final VectorProductSearchService vectorProductSearchService;
    private final MedoidDiversityService medoidDiversityService;

    public List<ProductRecommendResponse> recommendSimilarProducts(UUID productId, int topK) {
        // 1. 기준 상품 조회 및 벡터 추출
        ProductDocument product = productSearchRepository.findById(productId)
            .orElse(null);

        // 상품을 찾을 수 없는 경우 빈 리스트 반환 (에러를 던지지 않음)
        // MSA 환경에서 상품 디테일 페이지의 다른 부분이 정상 작동하도록 함
        if (product == null) {
            log.warn("유사 상품 추천을 위한 기준 상품을 찾을 수 없습니다. productId: {} (빈 리스트 반환)", productId);
            return List.of();
        }

        if (product.getVector() == null) {
            log.warn("기준 상품 '{}'의 벡터가 존재하지 않습니다. 임베딩이 필요합니다.", product.getProductName());
            return List.of();
        }

        float[] productVector = product.getVector();
        String productCategoryCode = product.getProductCategoryCode();

        // 2. 벡터 유사도 검색 실행 (같은 카테고리 보너스 적용) + 메도이드 다양성 적용
        return findSimilarProducts(productVector, productId, topK, productCategoryCode);
    }

    // 특정 벡터와 유사한 상품들을 Elasticsearch에서 검색한 뒤,
    // 메도이드 알고리즘을 적용해 다양성을 확보한 최종 추천 결과를 생성합니다.
    private List<ProductRecommendResponse> findSimilarProducts(
        float[] vector,
        UUID originalProductId,
        int topK,
        String originalCategoryCode
    ) {
        // 같은 카테고리 보너스 점수 설정 (0.3 = 30% 보너스)
        Double categoryMatchBonus = originalCategoryCode != null ? 0.3 : null;

        // 제외할 상품 ID 리스트 생성 (자기 자신 포함)
        List<UUID> excludeProductIds = originalProductId != null
            ? List.of(originalProductId)
            : List.of();

        // 메도이드 적용을 위해 topK보다 넉넉한 후보를 가져온다.
        int candidateSize = Math.min(topK * 3, 100);

        List<ProductDocument> candidates = vectorProductSearchService.findSimilarProductDocumentsByVector(
            vector,
            candidateSize,
            excludeProductIds,   // 자기 자신 제외
            originalCategoryCode,  // 기준 상품의 카테고리 코드
            categoryMatchBonus   // 카테고리 일치 보너스
        );

        if (candidates.isEmpty()) {
            return List.of();
        }

        // 메도이드 알고리즘으로 다양성을 확보한 대표 상품 선택
        List<ProductDocument> medoids =
            medoidDiversityService.selectDiverseMedoids(vector, candidates, topK);

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
        logRecommendationResults(
            recommendations, medoids, candidates, originalProductId, originalCategoryCode, categoryMatchBonus);

        return recommendations;
    }

    /**
     * 추천 결과와 근거를 로그로 출력합니다.
     */
    private void logRecommendationResults(
        List<ProductRecommendResponse> recommendations,
        List<ProductDocument> medoids,
        List<ProductDocument> candidates,
        UUID originalProductId,
        String originalCategoryCode,
        Double categoryMatchBonus
    ) {
        if (recommendations.isEmpty()) {
            log.info("🎯 [유사 상품 추천 결과] 추천된 상품이 없습니다. (기준 상품 ID: {})", originalProductId);
            return;
        }

        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format(
            "🎯 [유사 상품 추천 결과] 총 %d개 상품 추천 완료 (기준 상품 ID: %s)\n",
            recommendations.size(), originalProductId));
        logMessage.append(String.format("   - 후보 상품 수: %d개\n", candidates.size()));
        String categoryDisplay = originalCategoryCode != null ? originalCategoryCode : "없음";
        logMessage.append(String.format("   - 기준 상품 카테고리: %s\n", categoryDisplay));
        String bonusDisplay = categoryMatchBonus != null
            ? String.format("%.1f%%", categoryMatchBonus * 100)
            : "없음";
        logMessage.append(String.format("   - 카테고리 보너스: %s\n", bonusDisplay));
        logMessage.append("   - 추천 상품 목록:\n");

        for (int i = 0; i < recommendations.size(); i++) {
            ProductRecommendResponse rec = recommendations.get(i);
            ProductDocument medoid = medoids.get(i);

            // 카테고리 보너스 적용 여부 확인
            boolean hasCategoryBonus = originalCategoryCode != null
                && medoid.getProductCategoryCode() != null
                && medoid.getProductCategoryCode().equals(originalCategoryCode);

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
        log.debug("유사 상품 추천 결과 상세:\n{}", logMessage.toString());
    }
}
