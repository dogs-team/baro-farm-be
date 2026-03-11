package com.barofarm.user.auth.presentation.dto.oauth;

import com.barofarm.exception.CustomException;
import com.barofarm.user.auth.application.usecase.OAuthCallbackCommand;
import com.barofarm.user.auth.domain.oauth.OAuthProvider;
import com.barofarm.user.auth.exception.AuthErrorCode;
// import io.swagger.v3.oas.annotations.media.Schema;

public record OAuthCallbackRequest(
//     @Schema(description = "OAuth 공급자", example = "naver")
     String provider,
//     @Schema(description = "Authorization code")
     String code,
//     @Schema(description = "OAuth state")
     String state
) {

    public OAuthCallbackCommand toServiceRequest() {
        OAuthProvider resolved = OAuthProvider.from(provider);
        if (resolved == null) {
            throw new CustomException(AuthErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        }
        return new OAuthCallbackCommand(resolved, code, state);
    }
}
