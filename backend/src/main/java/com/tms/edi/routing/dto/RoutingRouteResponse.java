package com.tms.edi.routing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class RoutingRouteResponse {
    private Long routeId;
    private Long vehicleId;
    private String vehicleCode;
    private LocalDate routeDate;
    private Double totalDistanceM;
    private Double totalDurationS;
    private String status;
    private String optimizerRunId;
    private OffsetDateTime createdAt;
    private List<RoutingStopResponse> stops;
}
