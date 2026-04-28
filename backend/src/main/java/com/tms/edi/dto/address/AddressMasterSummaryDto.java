package com.tms.edi.dto.address;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressMasterSummaryDto {
    private Long addressId;
    private String addressCode;
    private String addressType;
    private String entityType;
    private Long entityId;
    private String city;
    private String postalCode;
    private String countryCode;
    private Boolean isPrimary;
    private Boolean isActive;
    private String validationStatus;
    private LocalDateTime updatedAt;
}