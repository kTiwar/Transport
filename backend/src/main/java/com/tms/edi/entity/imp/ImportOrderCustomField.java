package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "imp_order_custom_field")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderCustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_no", nullable = false)
    private Long entryNo;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "field_name", nullable = false, length = 200)
    private String fieldName;

    @Column(name = "field_value", length = 500)
    private String fieldValue;

    @Column(name = "external_order_no", length = 30)
    private String externalOrderNo;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "creation_date_time")
    private LocalDateTime creationDatetime;

    @Column(name = "last_modified_by", length = 50)
    private String lastModifiedBy;

    @Column(name = "last_modification_date_time")
    private LocalDateTime lastModificationDatetime;
}