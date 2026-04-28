package com.tms.edi.routing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
public class RoutingVehicleResponse {
    private Long vehicleId;
    private String code;
    private String vehicleType;
    private Double capacityWeightKg;
    private Double capacityVolumeM3;
    private Long startLocationId;
    private Long endLocationId;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private Boolean active;
    private OffsetDateTime createdAt;
}
