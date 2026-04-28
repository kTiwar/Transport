package com.tms.edi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mapping_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_line_id")
    private Long mappingLineId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mapping_id", nullable = false)
    private MappingHeader mappingHeader;

    @Column(name = "source_field_path", length = 500)
    private String sourceFieldPath;

    @Column(name = "target_field", nullable = false, length = 200)
    private String targetField;

    @Column(name = "transformation_rule", length = 200)
    private String transformationRule;

    /** Stored as JSON text; use Jackson in service layer to parse/serialize. */
    @Column(name = "transformation_params", columnDefinition = "TEXT")
    private String transformationParams;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "sequence", nullable = false)
    @Builder.Default
    private Integer sequence = 0;

    @Column(name = "condition_rule", columnDefinition = "TEXT")
    private String conditionRule;

    @Column(name = "lookup_table_name", length = 100)
    private String lookupTableName;
}
