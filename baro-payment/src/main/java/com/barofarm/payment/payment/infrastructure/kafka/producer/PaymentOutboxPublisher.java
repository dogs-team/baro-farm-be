package com.barofarm.payment.payment.infrastructure.kafka.producer;

import com.barofarm.payment.payment.domain.PaymentOutboxEvent;
import com.barofarm.payment.payment.domain.PaymentOutboxStatus;
import com.barofarm.payment.payment.infrastructure.PaymentOutboxEventJpaRepository;
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
public class PaymentOutboxPublisher {

    private static final long PROCESSING_TIMEOUT_SECONDS = 30L;

    private final PaymentOutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 2000L)
    public void publishOutboxEvents() {
        List<PaymentOutboxEvent> events = outboxRepository
            .findTop100ByStatusOrderByCreatedAtAsc(PaymentOutboxStatus.PENDING);

        for (PaymentOutboxEvent event : events) {
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

                log.error("Failed to publish payment outbox event. id={}, topic={}",
                    event.getId(), event.getTopic(), ex);
                markFailed(event.getId());
            });
        }
    }

    @Scheduled(fixedDelay = 10000L)
    public void recoverStuckProcessingEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(PROCESSING_TIMEOUT_SECONDS);
        List<PaymentOutboxEvent> stuckEvents = outboxRepository
            .findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(PaymentOutboxStatus.PROCESSING, cutoff);

        for (PaymentOutboxEvent event : stuckEvents) {
            markPending(event.getId());
            log.warn("Recovered stuck payment outbox event. id={}, topic={}",
                event.getId(), event.getTopic());
        }
    }

    private boolean markProcessing(java.util.UUID eventId) {
        Boolean result = transactionTemplate.execute(status -> {
            PaymentOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != PaymentOutboxStatus.PENDING) {
                return false;
            }

            event.markProcessing();
            return true;
        });
        return Boolean.TRUE.equals(result);
    }

    private void markSent(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != PaymentOutboxStatus.PROCESSING) {
                return;
            }

            event.markSent();
        });
    }

    private void markFailed(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != PaymentOutboxStatus.PROCESSING) {
                return;
            }

            event.markFailed();
        });
    }

    private void markPending(java.util.UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEvent event = outboxRepository.findById(eventId).orElse(null);
            if (event == null || event.getStatus() != PaymentOutboxStatus.PROCESSING) {
                return;
            }

            event.markPending();
        });
    }
}
