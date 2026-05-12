package com.barofarm.order.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "compensation_registry")
public class CompensationRegistry {

    @Id
    @Column(name = "id", columnDefinition = "BINARY(16)")
    private UUID id;

    private UUID orderId;

    @Enumerated(EnumType.STRING)
    private CompensationRegistryStatus status;

    private CompensationRegistry(UUID id, UUID orderId, CompensationRegistryStatus status){
        this.id = id;
        this.orderId = orderId;
        this.status = status;
    }

    public static CompensationRegistry of(UUID orderID){
        return new CompensationRegistry(
            UUID.randomUUID(),
            orderID,
            CompensationRegistryStatus.PENDING
        );
    }

    public boolean isPending() {
        return this.status == CompensationRegistryStatus.PENDING;
    }

    public void markComplete() {
        this.status = CompensationRegistryStatus.COMPLETE;
    }
}
