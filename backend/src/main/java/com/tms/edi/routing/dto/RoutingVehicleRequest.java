package com.tms.edi.routing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class RoutingVehicleRequest {

    @NotBlank
    private String code;

    private String vehicleType;

    @NotNull
    private Double capacityWeightKg = 1000.0;

    @NotNull
    private Double capacityVolumeM3 = 80.0;

    private Long startLocationId;
    private Long endLocationId;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
}
