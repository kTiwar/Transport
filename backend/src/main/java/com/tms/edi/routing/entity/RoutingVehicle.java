package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "routing_vehicle")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingVehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "vehicle_type", length = 64)
    private String vehicleType;

    @Column(name = "capacity_weight_kg", nullable = false)
    @Builder.Default
    private Double capacityWeightKg = 1000.0;

    @Column(name = "capacity_volume_m3", nullable = false)
    @Builder.Default
    private Double capacityVolumeM3 = 80.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_location_id")
    private RoutingLocation startLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_location_id")
    private RoutingLocation endLocation;

    @Column(name = "shift_start")
    private LocalTime shiftStart;

    @Column(name = "shift_end")
    private LocalTime shiftEnd;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
