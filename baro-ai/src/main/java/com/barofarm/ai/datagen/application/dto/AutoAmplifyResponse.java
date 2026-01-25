package com.barofarm.ai.datagen.application.dto;

import java.util.List;

// 자동 데이터 증폭 응답 DTO
public record AutoAmplifyResponse(
    int seedDataCount,
    int targetCount,
    int generatedCount,
    List<ProductGenerationDto> products
) {
}
