package com.tms.edi.dto.imp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportTmsLinkDto {
    private Long importEntryNo;
    private String externalOrderNo;
    private String tmsOrderNo;
    private String tmsPartitionExternalOrderNo;
}