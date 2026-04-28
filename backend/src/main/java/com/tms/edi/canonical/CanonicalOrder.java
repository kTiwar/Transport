package com.tms.edi.canonical;

import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalOrder {

    private String externalOrderId;
    private String customerCode;
    private LocalDate orderDate;
    private LocalDate requestedDeliveryDate;
    private String incoterm;
    private String priority;
    private String originAddress;
    private String destinationAddress;
    private String notes;

    @Builder.Default
    private List<CanonicalOrderLine> lines = new ArrayList<>();

    private CanonicalCargo cargo;
    private CanonicalTransport transportRequest;

    @Builder.Default
    private List<CanonicalCost> costs = new ArrayList<>();
}
