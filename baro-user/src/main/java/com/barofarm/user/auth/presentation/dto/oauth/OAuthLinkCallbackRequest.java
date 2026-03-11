package com.barofarm.user.auth.presentation.dto.oauth;

import com.barofarm.exception.CustomException;
import com.barofarm.user.auth.application.usecase.OAuthLinkCallbackCommand;
import com.barofarm.user.auth.domain.oauth.OAuthProvider;
import com.barofarm.user.auth.exception.AuthErrorCode;
import java.util.UUID;

public record OAuthLinkCallbackRequest(
//     @Schema(description = "OAuth 공급자", example = "naver")
     String provider,
//     @Schema(description = "Authorization code")
     String code,
//     @Schema(description = "OAuth link state")
     String state
) {

    public OAuthLinkCallbackCommand toServiceRequest(UUID userId) {
        OAuthProvider resolved = OAuthProvider.from(provider);
        if (resolved == null) {
            throw new CustomException(AuthErrorCode.OAUTH_UNSUPPORTED_PROVIDER);
        }
        return new OAuthLinkCallbackCommand(resolved, code, state, userId);
    }
}
