package com.tms.edi.ai.service;

import com.tms.edi.ai.engine.MappingEngine;
import com.tms.edi.ai.model.MappingCandidate;
import com.tms.edi.ai.model.SchemaField;
import com.tms.edi.ai.util.SimilarityUtil;
import com.tms.edi.dto.AiSuggestionDto;
import com.tms.edi.dto.SchemaTreeDto;
import com.tms.edi.entity.MappingLearning;
import com.tms.edi.entity.TmsFile;
import com.tms.edi.repository.MappingLearningRepository;
import com.tms.edi.repository.TmsFileRepository;
import com.tms.edi.service.StructureAnalyzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Mapping Service — orchestrates the full suggestion pipeline:
 *
 * <ol>
 *   <li>Load source file and analyse its schema structure.</li>
 *   <li>Flatten the schema tree to a list of {@link SchemaField}s.</li>
 *   <li>Resolve applicable historical learning boosts from the database.</li>
 *   <li>Delegate to {@link MappingEngine} for scored candidate generation.</li>
 *   <li>Assemble and return an {@link AiSuggestionDto} for the frontend.</li>
 * </ol>
 *
 * Learning is updated asynchronously when the user accepts or rejects suggestions
 * via {@link #recordAccepted} / {@link #recordRejected}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMappingService {

    private final TmsFileRepository       fileRepository;
    private final StructureAnalyzerService structureAnalyzer;
    private final MappingEngine           mappingEngine;
    private final MappingLearningRepository learningRepository;

    // ── Canonical target field definitions ────────────────────────────────────
    // Mirrors the CANONICAL_FIELDS constant in MappingDesignerPage.tsx.
    // required=true fields are marked with their field name in REQUIRED_TARGETS.

    private static final List<String> CANONICAL_TARGETS = List.of(
        // Order Header
        "communication_partner","transaction_type","external_order_number",
        "customer_no","customer_name","financial_status","operational_status",
        "reference1","reference2","reference3",
        // Order Totals
        "total_gross_weight","total_net_weight","total_volume","total_quantity","total_loading_meters",
        // Order References
        "references[].reference_code","references[].reference_value",
        // Truck / Driver / Trailer
        "truck.external_truck_id","truck.registration_number",
        "driver.external_driver_id","driver.driver_name",
        "trailer.external_trailer_id","trailer.registration_number",
        // Container Info
        "container.container_number","container.container_type","container.carrier_id",
        "container.carrier_name","container.seal_number","container.import_or_export",
        "container.pickup_pincode","container.pickup_reference",
        "container.dropoff_pincode","container.dropoff_reference",
        // Vessel Info
        "vessel.vessel_name","vessel.eta","vessel.etd",
        "vessel.origin_country","vessel.origin_port_name","vessel.origin_info",
        "vessel.destination_country","vessel.destination_port_name","vessel.destination_info",
        // Order Equipments
        "equipments[].material_type","equipments[].equipment_type",
        // Order Cargos
        "cargos[].external_good_id","cargos[].good_description",
        "cargos[].tracing_number1","cargos[].tracing_number2",
        "cargos[].quantity","cargos[].unit_of_measure",
        "cargos[].gross_weight","cargos[].net_weight","cargos[].volume",
        "cargos[].length","cargos[].width","cargos[].height",
        "cargos[].loading_meters","cargos[].diameter",
        "cargos[].dangerous_goods","cargos[].adr_un_number","cargos[].adr_classification",
        "cargos[].set_temperature","cargos[].temperature",
        // Order Remarks
        "remarks[].line_no","remarks[].remark_type","remarks[].remark",
        // Stop Lines
        "stop_lines[].external_order_line_id","stop_lines[].action",
        "stop_lines[].initial_dt_from","stop_lines[].initial_dt_until",
        "stop_lines[].booked_dt_from","stop_lines[].booked_dt_until",
        "stop_lines[].external_address_id","stop_lines[].address_name",
        "stop_lines[].address_street","stop_lines[].address_house_number",
        "stop_lines[].address_street2","stop_lines[].address_postal_code",
        "stop_lines[].address_city","stop_lines[].address_country_code",
        "stop_lines[].address_contact_person","stop_lines[].address_telephone",
        "stop_lines[].address_fax","stop_lines[].address_email",
        "stop_lines[].order_line_ref1","stop_lines[].order_line_ref2",
        "stop_lines[].part_of_order","stop_lines[].mileage","stop_lines[].order_line_status",
        "stop_lines[].requested_time_from","stop_lines[].requested_time_until",
        "stop_lines[].planned_time_from","stop_lines[].planned_time_until",
        "stop_lines[].driver_full_name","stop_lines[].truck_description",
        // Line Cargos
        "stop_lines[].line_cargos[].external_good_id","stop_lines[].line_cargos[].good_description",
        "stop_lines[].line_cargos[].good_type","stop_lines[].line_cargos[].good_sub_type",
        "stop_lines[].line_cargos[].tracing_number1","stop_lines[].line_cargos[].tracing_number2",
        "stop_lines[].line_cargos[].quantity","stop_lines[].line_cargos[].unit_of_measure",
        "stop_lines[].line_cargos[].gross_weight","stop_lines[].line_cargos[].net_weight",
        "stop_lines[].line_cargos[].volume","stop_lines[].line_cargos[].length",
        "stop_lines[].line_cargos[].width","stop_lines[].line_cargos[].height",
        "stop_lines[].line_cargos[].loading_meters","stop_lines[].line_cargos[].diameter",
        "stop_lines[].line_cargos[].dangerous_goods","stop_lines[].line_cargos[].adr_code",
        "stop_lines[].line_cargos[].set_temperature","stop_lines[].line_cargos[].temperature",
        // Line References & Remarks
        "stop_lines[].line_references[].reference_code","stop_lines[].line_references[].reference_value",
        "stop_lines[].line_remarks[].line_no","stop_lines[].line_remarks[].remark_type",
        "stop_lines[].line_remarks[].remark"
    );

    private static final Set<String> REQUIRED_TARGETS = Set.of(
        "communication_partner","customer_no",
        "equipments[].material_type","equipments[].equipment_type",
        "cargos[].quantity","cargos[].unit_of_measure",
        "stop_lines[].action"
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Generates AI mapping suggestions for a given source file.
     *
     * @param fileEntryNo  Primary key of the TmsFile to analyse
     * @param partnerCode  Optional partner code for targeted learning lookup (may be null)
     * @param fileType     Optional file type string for targeted learning lookup (may be null)
     * @return             Populated AiSuggestionDto ready for the frontend
     */
    public AiSuggestionDto suggest(Long fileEntryNo, String partnerCode, String fileType) {
        log.info("AI suggest requested for file={} partner={} type={}", fileEntryNo, partnerCode, fileType);

        // 1. Load and analyse the source file
        TmsFile file = fileRepository.findById(fileEntryNo)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + fileEntryNo));

        SchemaTreeDto schema;
        try {
            schema = structureAnalyzer.analyzeFile(file);
        } catch (Exception ex) {
            log.error("Schema analysis failed for file {}: {}", fileEntryNo, ex.getMessage());
            return emptyResult("Schema analysis failed: " + ex.getMessage());
        }

        // 2. Flatten schema tree -> List<SchemaField>
        List<SchemaField> flatFields = new ArrayList<>();
        flattenTree(schema, flatFields, 0);
        log.info("Extracted {} source fields from file {}", flatFields.size(), fileEntryNo);

        // 3. Load historical boosts
        Map<String, Double> boosts = loadHistoricalBoosts(partnerCode, fileType);
        log.info("Loaded {} historical learning boosts", boosts.size());

        // 4. Engine suggests best mappings
        List<MappingCandidate> candidates = mappingEngine.suggest(
                flatFields, CANONICAL_TARGETS, boosts, REQUIRED_TARGETS);

        // 5. Build AiSuggestionDto
        return toDto(candidates);
    }

    // ── Learning feedback ─────────────────────────────────────────────────────

    /**
     * Records that the user accepted a suggested mapping.
     * Updates or creates a learning record in the database.
     */
    @Transactional
    public void recordAccepted(String partnerCode, String fileType,
                                String sourceFieldPath, String targetField,
                                String transformationRule) {
        Optional<MappingLearning> existing = learningRepository
                .findByPartnerCodeAndFileTypeAndSourceFieldPathAndTargetField(
                        partnerCode, fileType, sourceFieldPath, targetField);

        if (existing.isPresent()) {
            existing.get().recordAcceptance();
            existing.get().setTransformationRule(transformationRule);
        } else {
            learningRepository.save(MappingLearning.builder()
                    .partnerCode(partnerCode)
                    .fileType(fileType)
                    .sourceFieldPath(sourceFieldPath)
                    .targetField(targetField)
                    .transformationRule(transformationRule)
                    .acceptedCount(1)
                    .confidenceBoost(0.10)
                    .lastAcceptedAt(OffsetDateTime.now())
                    .build());
        }
        log.debug("Recorded acceptance: {} -> {}", sourceFieldPath, targetField);
    }

    /**
     * Records that the user rejected a suggested mapping.
     */
    @Transactional
    public void recordRejected(String partnerCode, String fileType,
                                String sourceFieldPath, String targetField) {
        learningRepository.findByPartnerCodeAndFileTypeAndSourceFieldPathAndTargetField(
                partnerCode, fileType, sourceFieldPath, targetField)
                .ifPresent(m -> {
                    m.recordRejection();
                    log.debug("Recorded rejection: {} -> {}", sourceFieldPath, targetField);
                });
    }

    // ── Schema flattening ─────────────────────────────────────────────────────

    private void flattenTree(SchemaTreeDto node, List<SchemaField> out, int depth) {
        if (node == null) return;
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            // Leaf node -> add as source field
            out.add(SchemaField.builder()
                    .path(node.getPath())
                    .name(node.getName())
                    .type(inferType(node.getSampleValue(), node.getType()))
                    .sampleValue(node.getSampleValue())
                    .array(Boolean.TRUE.equals(node.getIsArray()))
                    .depth(depth)
                    .build());
        } else {
            // Internal node -> recurse into children
            for (SchemaTreeDto child : node.getChildren()) {
                flattenTree(child, out, depth + 1);
            }
        }
    }

    private String inferType(String sampleValue, String declaredType) {
        if (declaredType != null && !declaredType.isBlank()) return declaredType.toUpperCase();
        if (sampleValue == null || sampleValue.isBlank()) return "STRING";
        if (sampleValue.matches("[\\d.,+\\-]+"))               return "NUMBER";
        if (sampleValue.matches("\\d{4}-\\d{2}-\\d{2}.*"))    return "DATE";
        if (sampleValue.equalsIgnoreCase("true")
                || sampleValue.equalsIgnoreCase("false"))      return "BOOLEAN";
        return "STRING";
    }

    // ── Historical boosts ─────────────────────────────────────────────────────

    private Map<String, Double> loadHistoricalBoosts(String partnerCode, String fileType) {
        List<MappingLearning> records = (partnerCode != null && fileType != null)
                ? learningRepository.findApplicable(partnerCode, fileType)
                : learningRepository.findAllOrderByAcceptedCountDesc();

        Map<String, Double> boosts = new HashMap<>();
        for (MappingLearning r : records) {
            String key = r.getSourceFieldPath() + "|" + r.getTargetField();
            boosts.merge(key, r.getConfidenceBoost(), Double::max);
        }
        return boosts;
    }

    // ── DTO assembly ──────────────────────────────────────────────────────────

    private AiSuggestionDto toDto(List<MappingCandidate> candidates) {
        List<AiSuggestionDto.SuggestionItem> items = candidates.stream()
                .map(c -> AiSuggestionDto.SuggestionItem.builder()
                        .targetField(c.getTargetField())
                        .sourcePath(c.getSourceField().getPath())
                        .confidence(c.getConfidenceScore())
                        .reason(c.getReason())
                        .suggestedTransform(c.getSuggestedTransform())
                        .sourceType(c.getSourceField().getType())
                        .build())
                .collect(Collectors.toList());

        double overall = items.stream()
                .mapToDouble(AiSuggestionDto.SuggestionItem::getConfidence)
                .average().orElse(0.0);

        List<String> unmapped = REQUIRED_TARGETS.stream()
                .filter(t -> items.stream().noneMatch(i -> i.getTargetField().equals(t)))
                .collect(Collectors.toList());

        return AiSuggestionDto.builder()
                .suggestions(items)
                .overallConfidence(Math.round(overall * 1000.0) / 1000.0)
                .unmappedRequired(unmapped)
                .build();
    }

    private AiSuggestionDto emptyResult(String message) {
        return AiSuggestionDto.builder()
                .suggestions(List.of())
                .overallConfidence(0.0)
                .unmappedRequired(new ArrayList<>(REQUIRED_TARGETS))
                .build();
    }
}