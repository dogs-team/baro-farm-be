package com.barofarm.shopping.inventory.infrastructure.kafka.producer;

import com.barofarm.shopping.inventory.domain.InventoryOutboxEvent;
import com.barofarm.shopping.inventory.domain.InventoryOutboxStatus;
import com.barofarm.shopping.inventory.infrastructure.InventoryOutboxEventJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryOutboxPublisher {

    private static final long PROCESSING_TIMEOUT_SECONDS = 30L;

    private final InventoryOutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 2000L)
    public void publishInventoryEvents() {
        List<InventoryOutboxEvent> events =
            outboxRepository.findTop100ByStatusOrderByCreatedAtAsc(InventoryOutboxStatus.PENDING);

        for (InventoryOutboxEvent event : events) {
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

                log.error("Failed to publish inventory outbox event. id={}, topic={}",
                    event.getId(), event.getTopic(), ex);
                markFailed(event.getId());
            });
        }
    }

    @Scheduled(fixedDelay = 10000L)
    public void recoverStuckProcessingEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS);
        List<InventoryOutboxEvent> stuckEvents = outboxRepository
            .findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                InventoryOutboxStatus.PROCESSING,
                cutoff
            );

        for (InventoryOutboxEvent event : stuckEvents) {
            markPending(event.getId());
            log.warn("Recovered stuck inventory outbox event. id={}, topic={}",
                event.getId(), event.getTopic());
        }
    }

    private boolean markProcessing(java.util.UUID eventId) {
        Boolean result = transactionTemplate.execute(status -> {
            InventoryOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != InventoryOutboxStatus.PENDING) {
                return false;
            }

            event.markProcessing();
            return true;
        });
        return Boolean.TRUE.equals(result);
    }

    private void markSent(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            InventoryOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != InventoryOutboxStatus.PROCESSING) {
                return;
            }

            event.markSent();
        });
    }

    private void markFailed(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            InventoryOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != InventoryOutboxStatus.PROCESSING) {
                return;
            }

            event.markFailed();
        });
    }

    private void markPending(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            InventoryOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != InventoryOutboxStatus.PROCESSING) {
                return;
            }

            event.markPending();
        });
    }
}
