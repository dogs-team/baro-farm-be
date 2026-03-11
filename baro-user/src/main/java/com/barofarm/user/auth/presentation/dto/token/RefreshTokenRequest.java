package com.barofarm.user.auth.presentation.dto.token;

// import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenRequest(
//     @Schema(description = "리프레시 토큰", example = "refresh-token")
    String refreshToken) {}
