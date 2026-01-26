package com.barofarm.ai.search.application.dto.product;

import java.util.UUID;

// 프론트에 보여줄 상품 List Item
public record ProductSearchResponse(
    UUID productId,
    String productName, // 상품명
    String productCategoryName, // 카테고리 이름
    Long price // 가격
) {
}
