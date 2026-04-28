package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imp_order_remark")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderRemark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_no", nullable = false)
    private Long entryNo;

    @Column(name = "external_order_no", length = 30)
    private String externalOrderNo;

    @Column(name = "remark_type", length = 20)
    private String remarkType;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "remarks", length = 250)
    private String remarks;

    @Column(name = "external_remark_code", length = 50)
    private String externalRemarkCode;

    @Column(name = "import_date_time")
    private LocalDateTime importDatetime;

    @Column(name = "processed_date_time")
    private LocalDateTime processedDatetime;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "external_order_line_id", length = 20)
    private String externalOrderLineId;

    @Column(name = "order_line_no")
    private Integer orderLineNo;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "creation_date_time")
    private LocalDateTime creationDatetime;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "last_modification_date_time")
    private LocalDateTime lastModificationDatetime;
}