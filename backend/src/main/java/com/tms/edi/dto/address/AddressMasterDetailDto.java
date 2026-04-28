package com.tms.edi.dto.address;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressMasterDetailDto {
    private Long addressId;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<AddressContactDto> contacts;
    private List<AddressAttributeDto> attributes;
    private List<AddressUsageDto> usages;
    private List<AddressI18nDto> translations;
    private List<AddressAuditDto> auditTrail;
}