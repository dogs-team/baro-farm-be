package com.barofarm.order.order.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(UUID id);
    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Order> findTop100ByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
}
