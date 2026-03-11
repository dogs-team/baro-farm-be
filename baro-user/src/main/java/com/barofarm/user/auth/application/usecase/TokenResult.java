package com.barofarm.user.auth.application.usecase;

import java.util.UUID;

public record TokenResult(UUID userId, String email, String accessToken, String refreshToken) {
}
