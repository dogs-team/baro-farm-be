package com.barofarm.order.order.application;

import com.barofarm.order.order.domain.Order;
import com.barofarm.order.order.domain.OrderRepository;
import com.barofarm.order.order.domain.OrderStatus;
import com.barofarm.order.order.infrastructure.rest.InventoryClient;
import com.barofarm.order.order.infrastructure.rest.dto.InventoryCancelRequest;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AwaitingPaymentExpirationScheduler {

    private static final long EXPIRATION_MINUTES = 30L;

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final InventoryClient inventoryClient;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelay = 60000L)
    public void expireAwaitingPaymentOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);
        List<Order> expiredCandidates = orderRepository.findTop100ByStatusAndCreatedAtBefore(
            OrderStatus.AWAITING_PAYMENT,
            cutoff
        );

        for (Order order : expiredCandidates) {
            try {
                transactionTemplate.executeWithoutResult(status -> expireOrder(order));
            } catch (RuntimeException e) {
                log.warn("Awaiting payment expiration failed. orderId={}", order.getId(), e);
            }
        }
    }

    private void expireOrder(Order order) {
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return;
        }

        inventoryClient.cancelInventory(new InventoryCancelRequest(order.getId()));
        orderService.markExpired(order.getId());

        log.info("Expired awaiting payment order. orderId={}", order.getId());
    }
}
