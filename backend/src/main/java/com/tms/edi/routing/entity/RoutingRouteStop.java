package com.tms.edi.routing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "routing_route_stop")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stop_id")
    private Long stopId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RoutingRoute route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private RoutingDeliveryOrder order;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    /** DEPOT, PICKUP, DELIVERY */
    @Column(name = "stop_type", nullable = false, length = 16)
    private String stopType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private RoutingLocation location;

    @Column(name = "arrival_time")
    private OffsetDateTime arrivalTime;

    @Column(name = "departure_time")
    private OffsetDateTime departureTime;

    @Column(name = "travel_time_s")
    private Double travelTimeS;

    @Column(name = "distance_m")
    private Double distanceM;
}
