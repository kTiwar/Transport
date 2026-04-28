package com.tms.edi.dto.master;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressLookupOptionDto {
    private String category;
    private String code;
    private String name;
    private String description;
    private Map<String, String> extra;
}