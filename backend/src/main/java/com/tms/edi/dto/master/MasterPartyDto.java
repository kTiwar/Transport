package com.tms.edi.dto.master;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterPartyDto {
    private Long id;
    private String partyType;
    private String partyCode;
    private String name;
    private String legalName;
    private String vatNumber;
    private String countryCode;
    private String city;
    private String email;
    private String phone;
    private Boolean isActive;
}