package com.tms.edi.routing.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OptimizeRoutesRequest {

    /** Defaults to routing.default-depot from config if null. */
    private Double depotLatitude;
    private Double depotLongitude;

    @NotNull
    private LocalDate routeDate;

    @NotEmpty
    private List<Long> orderIds;

    @NotEmpty
    private List<Long> vehicleIds;
}
