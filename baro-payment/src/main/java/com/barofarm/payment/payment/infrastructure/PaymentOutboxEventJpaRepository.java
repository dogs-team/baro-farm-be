package com.barofarm.payment.payment.infrastructure;

import com.barofarm.payment.payment.domain.PaymentOutboxEvent;
import com.barofarm.payment.payment.domain.PaymentOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOutboxEventJpaRepository extends JpaRepository<PaymentOutboxEvent, UUID> {

    List<PaymentOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(PaymentOutboxStatus status);
    List<PaymentOutboxEvent> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
        PaymentOutboxStatus status,
        LocalDateTime cutoff
    );
}
