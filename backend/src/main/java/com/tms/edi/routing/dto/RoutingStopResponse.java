package com.tms.edi.routing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RoutingStopResponse {
    private Long stopId;
    private Integer sequenceNumber;
    private String stopType;
    private Long orderId;
    private Long locationId;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime arrivalTime;
    private OffsetDateTime departureTime;
    private Double travelTimeS;
    private Double distanceM;
}
