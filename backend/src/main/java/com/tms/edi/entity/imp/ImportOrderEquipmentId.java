package com.tms.edi.entity.imp;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportOrderEquipmentId implements Serializable {

    @Column(name = "entry_no")
    private Long entryNo;

    @Column(name = "line_no")
    private Integer lineNo;
}
