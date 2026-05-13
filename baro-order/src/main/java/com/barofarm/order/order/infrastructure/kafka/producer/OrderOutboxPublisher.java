package com.barofarm.order.order.infrastructure.kafka.producer;

import com.barofarm.order.order.domain.OrderOutboxEvent;
import com.barofarm.order.order.domain.OrderOutboxStatus;
import com.barofarm.order.order.infrastructure.OrderOutboxEventJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderOutboxPublisher {

    private static final long PROCESSING_TIMEOUT_SECONDS = 30L;

    private final OrderOutboxEventJpaRepository outboxRepository;
    @Qualifier("stringKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 2000L)
    public void publishOrderEvents() {
        List<OrderOutboxEvent> events =
            outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(OrderOutboxStatus.PENDING);

        for (OrderOutboxEvent event : events) {
            if (!markProcessing(event.getId())) {
                continue;
            }

            kafkaTemplate.send(
                event.getTopic(),
                event.getCorrelationId(),
                event.getPayload()
            ).whenComplete((result, ex) -> {
                if (ex == null) {
                    markSent(event.getId());
                    return;
                }

                log.error("Failed to publish order outbox event. id={}, topic={}",
                    event.getId(), event.getTopic(), ex);
                markFailed(event.getId());
            });
        }
    }

    @Scheduled(fixedDelay = 10000L)
    public void recoverStuckProcessingEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS);
        List<OrderOutboxEvent> stuckEvents = outboxRepository
            .findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(OrderOutboxStatus.PROCESSING, cutoff);

        for (OrderOutboxEvent event : stuckEvents) {
            markPending(event.getId());
            log.warn("Recovered stuck order outbox event. id={}, topic={}",
                event.getId(), event.getTopic());
        }
    }

    private boolean markProcessing(java.util.UUID eventId) {
        Boolean result = transactionTemplate.execute(status -> {
            OrderOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != OrderOutboxStatus.PENDING) {
                return false;
            }

            event.markProcessing();
            return true;
        });
        return Boolean.TRUE.equals(result);
    }

    private void markSent(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            OrderOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != OrderOutboxStatus.PROCESSING) {
                return;
            }

            event.markSent();
        });
    }

    private void markFailed(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            OrderOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != OrderOutboxStatus.PROCESSING) {
                return;
            }

            event.markFailed();
        });
    }

    private void markPending(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            OrderOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != OrderOutboxStatus.PROCESSING) {
                return;
            }

            event.markPending();
        });
    }
}
