package com.barofarm.ai.search.application;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.json.JsonData;
import com.barofarm.ai.log.application.LogWriteService;
import com.barofarm.ai.search.application.dto.product.ProductSearchRequest;
import com.barofarm.ai.search.application.dto.product.ProductSearchResponse;
import com.barofarm.ai.search.domain.ProductDocument;
import com.barofarm.dto.CustomPage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ElasticsearchOperations operations;
    private final LogWriteService logWriteService;
    private final ProductIndexService productIndexService;

    // 통합 검색을 위한 상품 검색 (키워드 하나만으로 검색)
    public CustomPage<ProductSearchResponse> searchProducts(UUID userId, String keyword, Pageable pageable) {

        NativeQuery query =
            NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // 키워드가 있는 경우에만 검색 조건을 추가
                    if (keyword != null && !keyword.isBlank()) {
                        applyExactMatch(b, keyword);
                        applyNormalMatch(b, keyword);

                        // 3글자 이상인 경우에만 오탈자 검색 허용
                        if (keyword.length() >= 3) {
                            applyFuzzyMatch(b, keyword);
                        }

                        // should 조건 중 최소 하나는 만족해야 검색 결과에 포함
                        b.minimumShouldMatch("1");
                    }
                    applyStatusFilter(b);

                    return b;
                }))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("updatedAt").order(SortOrder.Desc)))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);

        List<ProductSearchResponse> items =
            hits.getSearchHits().stream()
                .map(h -> h.getContent())
                .map(d -> new ProductSearchResponse(
                    d.getProductId(),
                    d.getProductName(),
                    d.getProductCategoryName(),
                    d.getPrice()
                ))
                .toList();

        // 🔹 "product 관련" 통합 검색 로그만 남긴다.
        // - userId가 있을 때만 개인화 추천용 로그 저장
        // - q가 비어있으면 검색 행동으로 간주하지 않음
        // - 로그 저장 실패가 검색 결과에는 영향을 주지 않음
        if (userId != null && keyword != null && !keyword.isBlank()) {
            try {
                logWriteService.saveSearchLog(
                    userId,
                    keyword,
                    null,
                    Instant.now()
                );
                // 프로필 벡터 비동기 업데이트
                productIndexService.updateUserProfileAsync(userId);
            } catch (Exception e) {
                log.warn("❌ Failed to save search log for user: " + userId + ", error: " + e.getMessage(), e);
            }
        }

        return CustomPage.of(hits.getTotalHits(), items, pageable);
    }

    // 상품 단독 검색 (필터링 조건 추가) + "상품 관련 사용자 행동 로그" 기록
    public CustomPage<ProductSearchResponse> searchOnlyProducts(
        UUID userId,
        ProductSearchRequest request,
        Pageable pageable
    ) {

        NativeQuery query =
            NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {

                    // 키워드가 있는 경우에만 검색 조건을 추가
                    if (request.keyword() != null && !request.keyword().isBlank()) {
                        String keyword = request.keyword();

                        applyExactMatch(b, keyword);
                        applyNormalMatch(b, keyword);

                        // 3글자 이상인 경우에만 오탈자 검색 허용
                        if (keyword.length() >= 3) {
                            applyFuzzyMatch(b, keyword);
                        }

                        // should 조건 중 최소 하나는 만족해야 검색 결과에 포함
                        b.minimumShouldMatch("1");
                    }
                    applyStatusFilter(b);
                    applyCategoryFilter(b, request.categories());
                    applyPriceFilter(b, request.priceMin(), request.priceMax());

                    return b;
                }))
                .withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)))
                .withSort(s -> s.field(f -> f.field("updatedAt").order(SortOrder.Desc)))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);

        List<ProductSearchResponse> items =
            hits.getSearchHits().stream()
                .map(h -> h.getContent())
                .map(d -> new ProductSearchResponse(
                    d.getProductId(),
                    d.getProductName(),
                    d.getProductCategoryName(),
                    d.getPrice()
                ))
                .toList();

        CustomPage<ProductSearchResponse> page =
            CustomPage.of(hits.getTotalHits(), items, pageable);

        // TODO 현재는 첫 번째 카테고리만 저장: 추후에 방안 고안
        String category =
            (request.categories() != null && !request.categories().isEmpty())
                ? request.categories().getFirst()
                : null;

        // 🔹 "product 관련" 검색 로그만 남긴다.
        // - UUID(userId)는 선택 사항: 존재할 때만 로그 저장
        // - keyword가 없거나 공백이면 검색 행동 로깅 대상에서 제외
        // - 로그 저장 실패가 검색 결과에는 영향을 주지 않음
        if (userId != null && request.keyword() != null && !request.keyword().isBlank()) {
            try {
                logWriteService.saveSearchLog(
                    userId,
                    request.keyword(),
                    category,
                    Instant.now()
                );
                // 프로필 벡터 비동기 업데이트
                productIndexService.updateUserProfileAsync(userId);
            } catch (Exception e) {
                log.warn("❌ Failed to save search log for user: " + userId +
                    ", keyword: " + request.keyword() + ", error: " + e.getMessage(), e);
            }
        }

        return page;
    }

    // 정확한 문구 검색
    private void applyExactMatch(BoolQuery.Builder b, String keyword) {
        b.should(m ->
            m.matchPhrase(mp ->
                mp.field("productName")
                  .query(keyword)
                  .slop(1)      // 단어 사이에 최대 1개 단어 차이 허용
                  .boost(2.0f)  // 가장 높은 가중치
            )
        );
    }

    // 일반 키워드 검색 (OR 조건)
    private void applyNormalMatch(BoolQuery.Builder b, String keyword) {
        b.should(m ->
            m.match(mm ->
                mm.field("productName")
                  .query(keyword)
                  .operator(Operator.Or)
                  .boost(1.0f)
            )
        );
    }

    // 오탈자 허용 검색
    private void applyFuzzyMatch(BoolQuery.Builder b, String keyword) {
        b.should(m ->
            m.match(mm ->
                mm.field("productName.raw")
                  .query(keyword)
                  .fuzziness("AUTO") // ES가 자동으로 편집 거리 계산
                  .prefixLength(1)   // 앞 글자 1개는 정확히 일치해야 함
                  .boost(0.3f)       // 가장 낮은 가중치
            )
        );
    }

    // 상품 상태 필터
    private void applyStatusFilter(BoolQuery.Builder b) {
        b.filter(f ->
            f.terms(t ->
                t.field("status")
                 .terms(v -> v.value(
                     List.of(
                         FieldValue.of("ON_SALE"),
                         FieldValue.of("DISCOUNTED") // 판매 중, 할인 중인 상품만
                     )
                 ))
            )
        );
    }

    // 카테고리 필터
    private void applyCategoryFilter(BoolQuery.Builder b, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return; // 카테고리 필터 없음
        }

        b.filter(f ->
            f.terms(t ->
                t.field("productCategoryId")
                    .terms(v ->
                        v.value(
                            categories.stream()
                                .map(this::categoryCodeToUuid)
                                .map(FieldValue::of)
                                .toList()
                        )
                    )
            )
        );
    }

    // 카테고리 코드를 UUID로 변환 (실제로는 DB 조회나 캐시에서 가져와야 함)
    private String categoryCodeToUuid(String categoryCode) {
        // 임시로 코드 그대로 반환 (실제로는 코드->UUID 매핑 필요)
        // TODO: 카테고리 코드에서 UUID로 변환하는 로직 구현
        return categoryCode;
    }

    // 가격 필터
    private void applyPriceFilter(BoolQuery.Builder b, Long priceMin, Long priceMax) {
        if (priceMin == null && priceMax == null) {
            return; // 가격 필터 없음
        }

        b.filter(f ->
            f.range(r -> {
                var range = r.field("price");
                if (priceMin != null) {
                    range = range.gte(JsonData.of(priceMin));
                }
                if (priceMax != null) {
                    range = range.lte(JsonData.of(priceMax));
                }
                return range;
            })
        );
    }
}
