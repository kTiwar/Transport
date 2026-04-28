package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "routing_vehicle_tracking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingVehicleTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private RoutingVehicle vehicle;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    void prePersist() {
        if (recordedAt == null) recordedAt = OffsetDateTime.now();
    }
}
