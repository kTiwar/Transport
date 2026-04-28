package com.tms.edi.routing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class RoutingDeliveryOrderRequest {

    private String publicOrderId;

    @NotBlank
    private String pickupAddress;

    private String pickupPostcode;

    @NotBlank
    private String deliveryAddress;

    private String deliveryPostcode;

    @NotNull
    private Double weightKg = 1.0;

    @NotNull
    private Double volumeM3 = 0.01;

    private OffsetDateTime timeWindowStart;
    private OffsetDateTime timeWindowEnd;
}
