package com.barofarm.user.auth.infrastructure.outbox;

import com.barofarm.user.auth.domain.outbox.OutboxEvent;
import com.barofarm.user.auth.domain.outbox.OutboxStatus;
import com.barofarm.user.auth.infrastructure.jpa.OutboxEventJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
// auth.outbox.publisher.enabled=true 일 때만 이 스케줄러 빈을 등록한다.
// 프로퍼티가 없으면(matchIfMissing = true) 기본값은 true 로 간주한다.
// 즉 운영/테스트 환경에서는 기존 동작을 유지하고, 로컬에서만 false 로 꺼서
// outbox_event polling SQL 로그를 잠시 숨길 수 있게 만든 설정이다.
@ConditionalOnProperty(prefix = "auth.outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventPublisher {
    /*
     * 로컬에서 JPA SQL 로그를 켜면 이 스케줄러가 주기적으로 outbox_event를 조회하면서
     * 같은 SELECT가 계속 출력된다.
     *
     * 운영/테스트 환경에서는 기본값(true)으로 두어 Outbox -> Kafka 발행 흐름을 유지하고,
     * 로컬에서 SQL 확인이 우선일 때만 auth.outbox.publisher.enabled=false 로 비활성화한다.
     */

    private final OutboxEventJpaRepository outboxEventRepository;
    private final KafkaTemplate<String, OutboxEventMessage> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${auth.kafka.topic.user-events:user-events}")
    private String topic;

    @Value("${auth.outbox.max-attempts:5}")
    private int maxAttempts;

    @Scheduled(fixedDelayString = "${auth.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPendingEvents() {
        // [0] 주기적으로 PENDING 이벤트를 스캔해 외부 브로커로 발행한다.
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        for (OutboxEvent event : pending) {
            try {
                OutboxEventMessage message = toMessage(event);
                // [1] 동기 전송으로 성공 여부를 확인한 뒤 상태를 갱신한다.
                kafkaTemplate.send(topic, message.getAggregateId(), message)
                    .get(5, TimeUnit.SECONDS);
                event.markPublished(LocalDateTime.now(clock));
            } catch (Exception ex) {
                // [2] 실패 시 횟수를 누적하고 한도를 넘기면 FAILED로 고정한다.
                event.markFailed(ex.getMessage(), maxAttempts);
            }
        }
    }

    private OutboxEventMessage toMessage(OutboxEvent event) {
        OutboxEventMessage message = new OutboxEventMessage();
        message.setEventId(event.getId().toString());
        message.setEventType(event.getEventType());
        message.setAggregateType(event.getAggregateType());
        message.setAggregateId(event.getAggregateId());
        message.setOccurredAt(event.getCreatedAt().toString());
        message.setPayload(parsePayload(event.getPayload()));
        return message;
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse outbox payload", ex);
        }
    }
}
