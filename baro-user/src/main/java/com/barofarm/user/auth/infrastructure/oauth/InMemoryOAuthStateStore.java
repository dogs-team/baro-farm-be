package com.barofarm.user.auth.infrastructure.oauth;

import com.barofarm.user.auth.application.port.out.OAuthStateStore;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InMemoryOAuthStateStore implements OAuthStateStore {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final Clock clock;
    private final String instanceId;
    private final Map<String, Instant> loginStates = new ConcurrentHashMap<>();
    private final Map<String, LinkState> linkStates = new ConcurrentHashMap<>();

    public InMemoryOAuthStateStore(Clock clock) {
        this.clock = clock;
        this.instanceId = ManagementFactory.getRuntimeMXBean().getName();
    }

    @Override
    public String issueLoginState() {
        // 로그인 콜백용 state는 단발성으로 발급한다.
        String state = UUID.randomUUID().toString();
        Instant expiresAt = expiresAt();
        loginStates.put(state, expiresAt);
        log.info("Issued OAuth login state. instanceId={}, state={}, expiresAt={}", instanceId, state, expiresAt);
        return state;
    }

    @Override
    public boolean validateLoginState(String state) {
        // 검증 후 바로 폐기하여 재사용을 막는다.
        Instant expiry = loginStates.remove(state);
        Instant now = Instant.now(clock);

        if (expiry == null) {
            log.warn("OAuth login state validation failed. instanceId={}, state={}, result=missing", instanceId, state);
            return false;
        }

        if (!expiry.isAfter(now)) {
            log.warn(
                "OAuth login state validation failed. instanceId={}, state={}, result=expired, expiresAt={}, now={}",
                instanceId,
                state,
                expiry,
                now
            );
            return false;
        }

        log.info(
            "OAuth login state validated. instanceId={}, state={}, result=valid, expiresAt={}, now={}",
            instanceId,
            state,
            expiry,
            now
        );
        return true;
    }

    @Override
    public String issueLinkState(UUID userId) {
        // 계정 연결 state는 사용자와 1:1로 매핑한다.
        String state = UUID.randomUUID().toString();
        Instant expiresAt = expiresAt();
        linkStates.put(state, new LinkState(userId, expiresAt));
        log.info(
            "Issued OAuth link state. instanceId={}, state={}, userId={}, expiresAt={}",
            instanceId,
            state,
            userId,
            expiresAt
        );
        return state;
    }

    @Override
    public UUID consumeLinkState(String state) {
        // 링크 state는 사용 즉시 제거해 CSRF 및 재사용을 방지한다.
        LinkState stored = linkStates.remove(state);
        Instant now = Instant.now(clock);
        if (stored == null) {
            log.warn(
                "OAuth link state consumption failed. instanceId={}, state={}, result=missing",
                instanceId,
                state
            );
            return null;
        }
        if (stored.expiresAt().isBefore(now)) {
            log.warn(
                "OAuth link state consumption failed. instanceId={}, state={}, result=expired, "
                    + "userId={}, expiresAt={}, now={}",
                instanceId,
                state,
                stored.userId(),
                stored.expiresAt(),
                now
            );
            return null;
        }
        log.info(
            "Consumed OAuth link state. instanceId={}, state={}, result=valid, userId={}, expiresAt={}, now={}",
            instanceId,
            state,
            stored.userId(),
            stored.expiresAt(),
            now
        );
        return stored.userId();
    }

    private Instant expiresAt() {
        return Instant.now(clock).plus(STATE_TTL);
    }

    private record LinkState(UUID userId, Instant expiresAt) {
    }
}
