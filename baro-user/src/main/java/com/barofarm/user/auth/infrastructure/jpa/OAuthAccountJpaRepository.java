package com.barofarm.user.auth.infrastructure.jpa;

import com.barofarm.user.auth.domain.oauth.OAuthAccount;
import com.barofarm.user.auth.domain.oauth.OAuthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccount, UUID> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    Optional<OAuthAccount> findByProviderAndUserId(OAuthProvider provider, UUID userId);

    void deleteAllByUserId(UUID userId);
}
