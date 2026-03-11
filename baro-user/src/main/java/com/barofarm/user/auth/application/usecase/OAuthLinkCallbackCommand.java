package com.barofarm.user.auth.application.usecase;

import com.barofarm.user.auth.domain.oauth.OAuthProvider;
import java.util.UUID;

public record OAuthLinkCallbackCommand(OAuthProvider provider, String code, String state, UUID userId) {
}
