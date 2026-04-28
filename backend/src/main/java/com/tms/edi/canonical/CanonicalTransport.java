package com.tms.edi.canonical;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CanonicalTransport {

    private String origin;
    private String destination;
    private String transportMode;
    private LocalDate pickupDate;
    private LocalDate deliveryDate;
    private String carrier;
    private String serviceLevel;
}
