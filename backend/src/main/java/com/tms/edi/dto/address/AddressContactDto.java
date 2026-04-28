package com.tms.edi.dto.address;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressContactDto {
    private Long contactId;
    private String contactName;
    private String phoneNumber;
    private String alternatePhone;
    private String email;
    private Boolean isPrimaryContact;
}