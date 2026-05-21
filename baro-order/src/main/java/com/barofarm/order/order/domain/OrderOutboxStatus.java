package com.barofarm.order.order.domain;

public enum OrderOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
