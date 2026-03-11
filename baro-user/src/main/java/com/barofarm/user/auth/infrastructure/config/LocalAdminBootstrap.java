package com.barofarm.user.auth.infrastructure.config;

import com.barofarm.user.auth.domain.credential.AuthCredential;
import com.barofarm.user.auth.domain.user.User;
import com.barofarm.user.auth.infrastructure.jpa.AuthCredentialJpaRepository;
import com.barofarm.user.auth.infrastructure.jpa.UserJpaRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.bootstrap.admin", name = "enabled", havingValue = "true")
public class LocalAdminBootstrap implements ApplicationRunner {

    /*
     * 로컬 개발 편의를 위한 bootstrap 코드다.
     * 애플리케이션 시작 시 지정한 admin 이메일이 없으면 계정/자격증명을 생성하고,
     * 이미 있으면 ADMIN 권한만 보정한다.
     *
     * 운영에서는 수동 DB 수정 대신 별도 생성 절차나 migration/seed를 두는 편이 안전하지만,
     * 로컬에서는 빠르게 관리자 기능을 검증할 수 있도록 profile=local 에서만 동작시킨다.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserJpaRepository userRepository;
    private final AuthCredentialJpaRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${auth.bootstrap.admin.email}")
    private String email;

    @Value("${auth.bootstrap.admin.password}")
    private String password;

    @Value("${auth.bootstrap.admin.name:Local Admin}")
    private String name;

    @Value("${auth.bootstrap.admin.phone:010-0000-0000}")
    private String phone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validatePasswordLength();

        User user = userRepository.findByEmail(email)
            .map(existing -> {
                if (existing.getUserType() != User.UserType.ADMIN) {
                    existing.changeToAdmin();
                    log.info("[BOOTSTRAP] 기존 계정을 ADMIN으로 승격합니다. email={}", email);
                } else {
                    log.info("[BOOTSTRAP] 기존 ADMIN 계정을 재사용합니다. email={}", email);
                }
                return existing;
            })
            .orElseGet(() -> {
                User created = User.create(email, name, phone, false);
                created.changeToAdmin();
                log.info("[BOOTSTRAP] 로컬 ADMIN 계정을 생성합니다. email={}", email);
                return userRepository.save(created);
            });

        userRepository.save(user);

        if (credentialRepository.existsByLoginEmail(email)) {
            return;
        }

        String salt = generateSalt();
        String encodedPassword = passwordEncoder.encode(password + salt);
        credentialRepository.save(AuthCredential.create(user.getId(), email, encodedPassword, salt));
        log.info("[BOOTSTRAP] 로컬 ADMIN 비밀번호 자격증명을 생성했습니다. email={}", email);
    }

    private String generateSalt() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private void validatePasswordLength() {
        /*
         * 현재 auth 서비스는 password + salt(64 hex chars)를 그대로 BCrypt에 넣는다.
         * BCrypt 입력은 최대 72 bytes 라서, ASCII 기준 비밀번호는 8자 이하여야 안전하다.
         * 로컬 bootstrap 비밀번호가 이 제한을 넘으면 애플리케이션 시작 시 즉시 실패시킨다.
         */
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes > 8) {
            throw new IllegalStateException(
                "Local admin bootstrap password is too long. "
                    + "Because BCrypt receives password+salt and the salt is 64 bytes in hex form, "
                    + "the local bootstrap password must be 8 bytes or fewer."
            );
        }
    }
}
