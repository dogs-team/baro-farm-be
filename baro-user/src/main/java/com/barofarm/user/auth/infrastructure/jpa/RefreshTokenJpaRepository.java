package com.barofarm.user.auth.infrastructure.jpa;

import com.barofarm.user.auth.domain.token.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUserId(UUID userId);
}
