package com.barofarm.user.auth.application.usecase;

import com.barofarm.user.auth.domain.oauth.OAuthProvider;

public record OAuthCallbackCommand(OAuthProvider provider, String code, String state) {
}
