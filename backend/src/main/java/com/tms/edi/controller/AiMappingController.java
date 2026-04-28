package com.tms.edi.controller;

import com.tms.edi.ai.engine.TransformationEngine;
import com.tms.edi.ai.service.AiMappingService;
import com.tms.edi.dto.*;
import com.tms.edi.entity.MappingLearning;
import com.tms.edi.repository.MappingLearningRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the AI Mapping and Transformation Engine.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /generate-mapping  — auto-suggest field mappings for a source file</li>
 *   <li>POST /transform         — apply a transformation rule to a raw value</li>
 *   <li>POST /save-mapping      — record user feedback (accept / reject) for learning</li>
 *   <li>GET  /mapping-history   — retrieve all stored learning records</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai-mapping")
@Tag(name = "AI Mapping Engine", description = "AI-powered field mapping suggestions and transformation engine")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AiMappingController {

    private final AiMappingService           aiMappingService;
    private final TransformationEngine       transformationEngine;
    private final MappingLearningRepository  learningRepository;

    // ── Generate mapping ──────────────────────────────────────────────────────

    @Operation(
        summary = "Auto-generate field mappings",
        description = "Analyses the source file schema and suggests best-match canonical target fields " +
                      "using name similarity, data-type heuristics, and historical learning."
    )
    @PostMapping("/generate-mapping")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<AiSuggestionDto> generateMapping(
            @RequestBody AiGenerateMappingRequest req) {

        log.info("AI generate-mapping: file={} partner={} type={}",
                req.getFileEntryNo(), req.getPartnerCode(), req.getFileType());

        AiSuggestionDto result = aiMappingService.suggest(
                req.getFileEntryNo(), req.getPartnerCode(), req.getFileType());
        return ResponseEntity.ok(result);
    }

    // ── Transform ─────────────────────────────────────────────────────────────

    @Operation(
        summary = "Apply a transformation rule",
        description = "Applies a single rule or a JSON rule chain to a raw source value. " +
                      "Supported rules: DIRECT, CONSTANT, UPPER, LOWER, TRIM, SUBSTRING, CONCAT, " +
                      "REPLACE, DATE_FORMAT, DATE_NOW, ROUND, MATH, TO_NUMBER, IF, IF_NULL, LOOKUP."
    )
    @PostMapping("/transform")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<AiTransformResponse> transform(
            @RequestBody AiTransformRequest req) {

        try {
            Object result = (req.getRuleChain() != null && !req.getRuleChain().isBlank())
                    ? transformationEngine.applyChain(req.getRuleChain(), req.getValue())
                    : transformationEngine.applyRule(req.getRule(), req.getValue(), req.getParams());

            return ResponseEntity.ok(AiTransformResponse.builder()
                    .originalValue(req.getValue())
                    .transformedValue(result != null ? result.toString() : null)
                    .ruleApplied(req.getRuleChain() != null ? "CHAIN" : req.getRule())
                    .success(true)
                    .build());

        } catch (Exception ex) {
            log.warn("Transform failed: {}", ex.getMessage());
            return ResponseEntity.ok(AiTransformResponse.builder()
                    .originalValue(req.getValue())
                    .transformedValue(req.getValue())
                    .ruleApplied(req.getRule())
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .build());
        }
    }

    // ── Save mapping (learning feedback) ──────────────────────────────────────

    @Operation(
        summary = "Record mapping acceptance or rejection",
        description = "Registers user feedback so the AI engine can boost or penalise a " +
                      "source-to-target pairing in future suggestions (online learning)."
    )
    @PostMapping("/save-mapping")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, String>> saveMapping(
            @RequestBody SaveLearningRequest req,
            @AuthenticationPrincipal String username) {

        if (req.isAccepted()) {
            aiMappingService.recordAccepted(
                    req.getPartnerCode(), req.getFileType(),
                    req.getSourceFieldPath(), req.getTargetField(),
                    req.getTransformationRule());
        } else {
            aiMappingService.recordRejected(
                    req.getPartnerCode(), req.getFileType(),
                    req.getSourceFieldPath(), req.getTargetField());
        }

        log.info("Learning feedback by {}: {} {} -> {}",
                username, req.isAccepted() ? "ACCEPTED" : "REJECTED",
                req.getSourceFieldPath(), req.getTargetField());

        return ResponseEntity.ok(Map.of("status", "recorded"));
    }

    // ── Mapping history ───────────────────────────────────────────────────────

    @Operation(
        summary = "Retrieve mapping learning history",
        description = "Returns all stored accepted mappings sorted by acceptance count descending. " +
                      "Use partner_code and file_type query params to filter."
    )
    @GetMapping("/mapping-history")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<List<MappingLearning>> getMappingHistory(
            @RequestParam(required = false) String partnerCode,
            @RequestParam(required = false) String fileType) {

        List<MappingLearning> records = (partnerCode != null && fileType != null)
                ? learningRepository.findApplicable(partnerCode, fileType)
                : learningRepository.findAllOrderByAcceptedCountDesc();

        return ResponseEntity.ok(records);
    }

    // ── Infer transform ───────────────────────────────────────────────────────

    @Operation(summary = "Infer suggested transformation rule",
               description = "Given a target field name and a sample value, suggests the best rule.")
    @GetMapping("/infer-transform")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Map<String, String>> inferTransform(
            @RequestParam String targetField,
            @RequestParam(required = false, defaultValue = "") String sampleValue) {

        String rule = transformationEngine.inferTransform(targetField, sampleValue);
        return ResponseEntity.ok(Map.of("rule", rule, "targetField", targetField));
    }
}