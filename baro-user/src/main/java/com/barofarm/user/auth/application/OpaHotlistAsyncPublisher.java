package com.barofarm.user.auth.application;

import com.barofarm.user.auth.application.event.HotlistEventMessage;
import com.barofarm.user.auth.application.port.HotlistEventPublisher;
import com.barofarm.user.auth.domain.user.SellerStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpaHotlistAsyncPublisher {

    private final HotlistEventPublisher hotlistEventPublisher;

    @Async("opaEventExecutor")
    public void publishSellerStatusEvent(String sellerId, SellerStatus status, String reason) {
        /*
         * 승인/정지 요청은 DB 커밋이 끝난 뒤 바로 응답을 반환하고,
         * OPA 반영은 별도 스레드에서 후속 처리한다.
         * 외부 브로커 지연이 사용자 요청 타임아웃으로 이어지지 않게 하려는 분리다.
         */
        try {
            HotlistEventMessage event = new HotlistEventMessage();
            event.setEventId(UUID.randomUUID().toString());
            event.setSubjectType("seller");
            event.setSubjectId(sellerId);
            event.setActive(true);
            event.setStatus(status.name());
            event.setFlags(List.of());
            event.setReason(reason == null || reason.isBlank() ? "SELLER_STATUS_UPDATE" : reason.trim());
            event.setUpdatedAt(Instant.now().toString());
            hotlistEventPublisher.publish(event);
        } catch (Exception ex) {
            log.error("[AUTH] seller 상태 변경 이후 OPA hotlist 이벤트 전파에 실패했습니다. sellerId={}", sellerId, ex);
        }
    }
}
