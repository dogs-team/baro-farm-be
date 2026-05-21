package com.barofarm.order.order.domain;

public enum OrderStatus {
    // 주문 생성(초기값)
    PENDING,

    // 재고 예약 완료
    AWAITING_PAYMENT,

    // 결제/재고 확정 완료
    CONFIRMED,

    // 주문 취소 진행 중
    CANCEL_PENDING,

    // 주문 취소 완료
    CANCELED,

    // 주문 실패(결제/재고 등 도메인 실패)
    FAILED,

    EXPIRED,

    // 배송 준비 중(환불 불가)
    PREPARING,

    // 배송 완료(환불 불가)
    COMPLETED;

    public boolean isCancelable() {
        return this == CONFIRMED;
    }

    public boolean isCanceled() {
        return this == CANCELED;
    }

    public boolean isReviewable() {
        return this == COMPLETED;
    }
}
