package com.tms.edi.dto.imp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderEquipmentDto {

    private Long entryNo;
    private Integer lineNo;
    private String externalOrderNo;
    private String materialType;
    private String equipmentTypeNo;
    private String equipmentSubTypeNo;
    private Boolean customizedBoolean;
    private String source;
    private String remark;
    private BigDecimal tareWeight;
    private BigDecimal vgmWeight;
    private LocalDateTime importDatetime;
    private LocalDateTime processedDatetime;
    private String communicationPartner;
    private String externalOrderLineId;
    private Integer orderLineNo;
    private String cleaningInstruction;
    private String createdBy;
    private LocalDateTime creationDatetime;
    private String lastModifiedBy;
    private LocalDateTime lastModificationDatetime;
    private Integer quantity;
}