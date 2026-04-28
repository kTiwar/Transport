package com.tms.edi.dto.address;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressAuditDto {
    private Long auditId;
    private String changedBy;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;
}