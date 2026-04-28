package com.tms.edi.dto.address;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressMasterUpsertDto {
    private String addressCode;
    private String addressType;
    private String entityType;
    private Long entityId;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String landmark;
    private String city;
    private String district;
    private String stateProvince;
    private String postalCode;
    private String countryCode;
    private String countryName;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private Boolean isPrimary;
    private Boolean isActive;
    private String validationStatus;
}