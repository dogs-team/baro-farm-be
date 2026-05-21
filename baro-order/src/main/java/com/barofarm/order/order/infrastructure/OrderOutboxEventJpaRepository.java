package com.barofarm.order.order.infrastructure;

import com.barofarm.order.order.domain.OrderOutboxEvent;
import com.barofarm.order.order.domain.OrderOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOutboxEventJpaRepository extends JpaRepository<OrderOutboxEvent, UUID> {

    List<OrderOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OrderOutboxStatus status);
    List<OrderOutboxEvent> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
        OrderOutboxStatus status,
        LocalDateTime cutoff
    );
}
