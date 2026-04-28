package com.tms.edi.dto.imp;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderReferenceDto {
    private Long entryNo;
    private Integer lineNo;
    private String referenceCode;
    private String reference;
    private Integer orderLineNo;
}
