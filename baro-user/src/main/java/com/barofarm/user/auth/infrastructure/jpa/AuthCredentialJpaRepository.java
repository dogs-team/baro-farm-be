package com.barofarm.user.auth.infrastructure.jpa;

import com.barofarm.user.auth.domain.credential.AuthCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthCredentialJpaRepository extends JpaRepository<AuthCredential, UUID> {

    boolean existsByLoginEmail(String loginEmail);

    Optional<AuthCredential> findByLoginEmail(String loginEmail);

    Optional<AuthCredential> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
