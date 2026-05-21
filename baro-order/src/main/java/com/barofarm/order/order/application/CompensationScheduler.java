package com.barofarm.order.order.application;

import com.barofarm.order.order.domain.CompensationRegistry;
import com.barofarm.order.order.domain.CompensationRegistryRepository;
import com.barofarm.order.order.infrastructure.rest.InventoryClient;
import com.barofarm.order.order.infrastructure.rest.dto.InventoryCancelRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompensationScheduler {

    private final CompensationRegistryRepository compensationRegistryRepository;
    private final OrderService orderService;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 10000L)
    public void retryPendingCompensations() {
        List<CompensationRegistry> pendingCompensations =
            compensationRegistryRepository.findTop100ByStatusPending();

        for (CompensationRegistry compensation : pendingCompensations) {
            try {
                transactionTemplate.executeWithoutResult(status -> retryCompensation(compensation));
            } catch (RuntimeException e) {
                log.warn("Compensation retry failed. orderId={}", compensation.getOrderId(), e);
            }
        }
    }

    private void retryCompensation(CompensationRegistry compensation) {
        if (!compensation.isPending()) {
            return;
        }

        orderService.markFailed(compensation.getOrderId());
        inventoryClient.cancelInventory(new InventoryCancelRequest(compensation.getOrderId()));
        compensation.markComplete();

        log.info("Completed compensation retry. orderId={}", compensation.getOrderId());
    }
}
