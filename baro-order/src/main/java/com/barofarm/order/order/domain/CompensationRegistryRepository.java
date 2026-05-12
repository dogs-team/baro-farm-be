package com.barofarm.order.order.domain;

import java.util.List;

public interface CompensationRegistryRepository {
    CompensationRegistry save(CompensationRegistry compensationRegistry);
    List<CompensationRegistry> findTop100ByStatusPending();
}
