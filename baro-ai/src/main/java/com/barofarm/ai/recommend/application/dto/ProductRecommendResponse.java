package com.barofarm.ai.recommend.application.dto;

import java.util.UUID;

// 일반 상품 추천 응답 DTO (유사도 점수 없음)
// 3개의 추천 서비스 모두에서 사용됨.
public record ProductRecommendResponse(
    UUID productId,        // 상품 고유 식별자
    String productName,     // 상품 이름
    String productCategoryName, // 상품 카테고리 이름
    Long price              // 상품 가격
) { }
