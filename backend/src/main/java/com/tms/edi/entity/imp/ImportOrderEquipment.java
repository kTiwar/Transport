package com.tms.edi.entity.imp;

import jakarta.persistence.*;
import lombok.*;

/**
 * Staging table for EDI equipment requirements.
 * Maps to AL: "Go4Imp Import Order Eq"
 */
@Entity
@Table(name = "imp_order_equipment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportOrderEquipment {

    @EmbeddedId
    private ImportOrderEquipmentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_no", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ImportOrderHeader header;

    @Column(name = "communication_partner", length = 20)
    private String communicationPartner;

    @Column(name = "external_order_no", length = 30)
    private String externalOrderNo;

    @Column(name = "equipment_type_no", length = 50)
    private String equipmentTypeNo;

    @Column(name = "equipment_sub_type_no", length = 50)
    private String equipmentSubTypeNo;

    /** '-', 'FULL', 'EMPTY' */
    @Column(name = "material_type", length = 20)
    @Builder.Default
    private String materialType = "-";

    @Column(name = "quantity")
    private Integer quantity;
}
