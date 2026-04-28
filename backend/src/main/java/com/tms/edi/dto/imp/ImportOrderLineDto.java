package com.tms.edi.dto.imp;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderLineDto {
    private Long entryNo;
    private Integer lineNo;
    /** When set, may split processing into multiple TMS orders (distinct values → one TMS per value). */
    private String externalOrderNo;
    private String actionCode;
    private String externalAddressNo;
    private String addressName;
    private String addressCity;
    private String addressCountryCode;
    private String addressPostalCode;
    private LocalDateTime initialDatetimeFrom;
    private LocalDateTime initialDatetimeUntil;
    private LocalDateTime requestedDatetimeFrom;
    private LocalDateTime requestedDatetimeUntil;
    private String containerNo;
    private Boolean loaded;
    private String orderLineRef1;
    private String orderLineRef2;
}
