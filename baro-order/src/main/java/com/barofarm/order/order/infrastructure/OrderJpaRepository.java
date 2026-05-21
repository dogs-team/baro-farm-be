package com.barofarm.order.order.infrastructure;

import com.barofarm.order.order.domain.Order;
import com.barofarm.order.order.domain.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Order> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
        OrderStatus status,
        LocalDateTime cutoff
    );
}
