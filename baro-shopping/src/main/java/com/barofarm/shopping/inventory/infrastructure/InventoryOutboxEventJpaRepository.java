package com.barofarm.shopping.inventory.infrastructure;

import com.barofarm.shopping.inventory.domain.InventoryOutboxEvent;
import com.barofarm.shopping.inventory.domain.InventoryOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryOutboxEventJpaRepository extends JpaRepository<InventoryOutboxEvent, UUID> {

    List<InventoryOutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(InventoryOutboxStatus status);
    List<InventoryOutboxEvent> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
        InventoryOutboxStatus status,
        LocalDateTime cutoff
    );
}
