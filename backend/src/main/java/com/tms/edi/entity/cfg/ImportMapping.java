package com.tms.edi.entity.cfg;

import com.tms.edi.enums.MappingType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Lookup table translating external partner codes to internal TMS codes.
 * Maps to AL: "Go4IMP Import Mapping"
 *
 * Example:
 *  Partner=ACME, Type=CUSTOMER, ForeignId="CUST001" → LocalId="C0042"
 *  Partner=ACME, Type=ADDRESS,  ForeignId="ADR99"   → LocalId="A0017"
 */
@Entity
@Table(name = "cfg_import_mapping",
       uniqueConstraints = @UniqueConstraint(columnNames = {"communication_partner", "mapping_type", "foreign_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "communication_partner", nullable = false, length = 20)
    private String communicationPartner;

    @Enumerated(EnumType.STRING)
    @Column(name = "mapping_type", nullable = false, length = 30)
    private MappingType mappingType;

    /** The code/value as it arrives from the external partner */
    @Column(name = "foreign_id", nullable = false, length = 100)
    private String foreignId;

    /** The corresponding internal TMS code */
    @Column(name = "local_id", nullable = false, length = 100)
    private String localId;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;
}
