package com.tms.edi.controller;

import com.tms.edi.ai.service.AiMappingService;
import com.tms.edi.dto.*;
import com.tms.edi.service.AuditService;
import com.tms.edi.service.MappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mappings")
@Tag(name = "Mappings", description = "Mapping template management")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MappingController {

    private final MappingService   mappingService;
    private final AuditService     auditService;
    private final AiMappingService aiMappingService;

    @Operation(summary = "List mappings, optionally filtered by partner")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<List<MappingDto>> listMappings(
            @RequestParam(required = false) Long partnerId) {
        if (partnerId != null) {
            return ResponseEntity.ok(mappingService.findByPartner(partnerId));
        }
        return ResponseEntity.ok(List.of());
    }

    @Operation(summary = "Get full mapping detail including all lines")
    @GetMapping("/{mappingId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<MappingDto> getMapping(@PathVariable Long mappingId) {
        return ResponseEntity.ok(mappingService.findById(mappingId));
    }

    @Operation(summary = "Create a new mapping template")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<MappingDto> createMapping(
            @RequestBody MappingDto dto,
            @AuthenticationPrincipal String username) {

        MappingDto created = mappingService.createMapping(dto, username);
        auditService.log(username, "MAPPING_CREATED", "MAPPING",
                created.getMappingId().toString(), "Created " + dto.getMappingName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Save mapping lines (creates version snapshot)")
    @PutMapping("/{mappingId}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<MappingDto> updateMapping(
            @PathVariable Long mappingId,
            @RequestBody MappingDto dto,
            @AuthenticationPrincipal String username) {

        MappingDto updated = mappingService.updateMappingLines(mappingId, dto, username);
        auditService.log(username, "MAPPING_UPDATED", "MAPPING",
                mappingId.toString(), "Saved v" + updated.getVersion() +
                        " with " + updated.getLines().size() + " lines");
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get version history for a mapping")
    @GetMapping("/{mappingId}/versions")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<List<MappingVersionHistoryDto>> getVersionHistory(
            @PathVariable Long mappingId) {
        return ResponseEntity.ok(mappingService.getVersionHistory(mappingId));
    }

    @Operation(summary = "Activate a mapping version")
    @PostMapping("/{mappingId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> activateMapping(
            @PathVariable Long mappingId,
            @AuthenticationPrincipal String username) {

        mappingService.activateMapping(mappingId);
        auditService.log(username, "MAPPING_ACTIVATED", "MAPPING",
                mappingId.toString(), "Mapping activated");
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Get AI mapping suggestions for a file",
        description = "Analyses the source file schema and returns confidence-scored canonical " +
                      "target suggestions using name similarity, sample-data heuristics, and historical learning."
    )
    @PostMapping("/ai-suggest")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<AiSuggestionDto> aiSuggest(
            @RequestBody AiSuggestRequest req) {
        AiSuggestionDto result = aiMappingService.suggest(
                req.entryNo(), req.partnerCode(), req.fileType());
        return ResponseEntity.ok(result);
    }

    public record AiSuggestRequest(Long entryNo, String partnerCode, String fileType) {}
}
