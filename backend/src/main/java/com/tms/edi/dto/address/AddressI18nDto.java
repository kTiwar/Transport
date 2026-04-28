package com.tms.edi.dto.address;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressI18nDto {
    private Long id;
    private String languageCode;
    private String addressText;
}