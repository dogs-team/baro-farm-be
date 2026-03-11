package com.barofarm.user.auth.application.port.out;

import com.barofarm.user.auth.domain.oauth.OAuthProvider;
import com.barofarm.user.auth.domain.oauth.OAuthUserInfo;

/**
 * OAuth 공급자(카카오/네이버 등) 사용자 정보 조회 포트 (아웃바운드).
 */
public interface OAuthProviderClient {

    OAuthUserInfo fetchUserInfo(OAuthProvider provider, String code, String state);
}
