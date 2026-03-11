package com.barofarm.user.auth.application.port.out;

import java.util.UUID;

/**
 * OAuth state(CSRF) 저장/검증 포트 (아웃바운드).
 */
public interface OAuthStateStore {

    String issueLoginState();

    boolean validateLoginState(String state);

    String issueLinkState(UUID userId);

    UUID consumeLinkState(String state);
}
