package com.tms.edi.routing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class RoutingDeliveryOrderResponse {
    private Long id;
    private String publicOrderId;
    private String pickupAddress;
    private String pickupPostcode;
    private String deliveryAddress;
    private String deliveryPostcode;
    private Double weightKg;
    private Double volumeM3;
    private OffsetDateTime timeWindowStart;
    private OffsetDateTime timeWindowEnd;
    private Long pickupLocationId;
    private Long deliveryLocationId;
    private Long tmsOrderId;
    private String tmsOrderNo;
    private String status;
    private OffsetDateTime createdAt;
}
