package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routing_route")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "route_id")
    private Long routeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private RoutingVehicle vehicle;

    @Column(name = "route_date", nullable = false)
    private LocalDate routeDate;

    @Column(name = "total_distance_m")
    private Double totalDistanceM;

    @Column(name = "total_duration_s")
    private Double totalDurationS;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "PLANNED";

    @Column(name = "optimizer_run_id", length = 64)
    private String optimizerRunId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("sequenceNumber ASC")
    @Builder.Default
    private List<RoutingRouteStop> stops = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
