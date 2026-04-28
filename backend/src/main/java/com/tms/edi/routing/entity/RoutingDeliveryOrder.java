package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "routing_delivery_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingDeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_order_id", unique = true, length = 64)
    private String publicOrderId;

    @Column(name = "pickup_address", nullable = false, columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "pickup_postcode", length = 32)
    private String pickupPostcode;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Column(name = "delivery_postcode", length = 32)
    private String deliveryPostcode;

    @Column(name = "weight_kg", nullable = false)
    @Builder.Default
    private Double weightKg = 1.0;

    @Column(name = "volume_m3", nullable = false)
    @Builder.Default
    private Double volumeM3 = 0.01;

    @Column(name = "time_window_start")
    private OffsetDateTime timeWindowStart;

    @Column(name = "time_window_end")
    private OffsetDateTime timeWindowEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_location_id")
    private RoutingLocation pickupLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_location_id")
    private RoutingLocation deliveryLocation;

    @Column(name = "tms_order_id")
    private Long tmsOrderId;

    @Column(name = "tms_order_no", length = 20)
    private String tmsOrderNo;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
