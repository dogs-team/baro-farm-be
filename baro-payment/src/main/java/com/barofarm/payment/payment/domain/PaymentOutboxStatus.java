package com.barofarm.payment.payment.domain;

public enum PaymentOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
