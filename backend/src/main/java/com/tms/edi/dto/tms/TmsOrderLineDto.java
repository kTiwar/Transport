package com.tms.edi.dto.tms;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TmsOrderLineDto {
    private Long id;
    private Integer lineNo;
    private String actionCode;
    private String addressNo;
    private LocalDateTime initialDatetimeFrom;
    private LocalDateTime initialDatetimeUntil;
    private LocalDateTime requestedDatetimeFrom;
    private LocalDateTime requestedDatetimeUntil;
    private String containerNo;
    private Boolean loaded;
}
