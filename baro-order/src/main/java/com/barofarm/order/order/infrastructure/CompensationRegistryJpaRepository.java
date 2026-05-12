package com.barofarm.order.order.infrastructure;

import com.barofarm.order.order.domain.CompensationRegistry;
import com.barofarm.order.order.domain.CompensationRegistryStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompensationRegistryJpaRepository extends JpaRepository<CompensationRegistry, UUID> {
    List<CompensationRegistry> findTop100ByStatusOrderByIdAsc(CompensationRegistryStatus status);
}
