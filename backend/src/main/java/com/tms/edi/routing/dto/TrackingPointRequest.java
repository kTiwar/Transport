package com.tms.edi.routing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrackingPointRequest {
    @NotNull
    private Double latitude;
    @NotNull
    private Double longitude;
}
