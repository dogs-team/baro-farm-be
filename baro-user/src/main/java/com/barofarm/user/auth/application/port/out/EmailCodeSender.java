package com.barofarm.user.auth.application.port.out;

/**
 * 이메일 인증 코드 발송 포트 (아웃바운드).
 */
public interface EmailCodeSender {

    void send(String email, String code);
}
