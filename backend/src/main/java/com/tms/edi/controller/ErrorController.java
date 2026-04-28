package com.tms.edi.controller;

import com.tms.edi.dto.ErrorLogDto;
import com.tms.edi.entity.EdiErrorLog;
import com.tms.edi.exception.EdiException;
import com.tms.edi.repository.EdiErrorLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/errors")
@Tag(name = "Errors", description = "EDI processing error log and resolution")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ErrorController {

    private final EdiErrorLogRepository errorLogRepository;

    @Operation(summary = "List all errors with filtering")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<Page<ErrorLogDto>> listErrors(
            @RequestParam(defaultValue = "0")     int page,
            @RequestParam(defaultValue = "50")    int size,
            @RequestParam(defaultValue = "false") boolean resolvedOnly) {

        // resolvedOnly=false → unresolved only; resolvedOnly=true → all (resolved + unresolved)
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<EdiErrorLog> errors = resolvedOnly
                ? errorLogRepository.findAll(pageable)
                : errorLogRepository.findByResolvedFlagFalse(pageable);
        return ResponseEntity.ok(errors.map(this::toDto));
    }

    @Operation(summary = "Get errors for a specific file")
    @GetMapping("/file/{entryNo}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<?> getFileErrors(@PathVariable Long entryNo) {
        return ResponseEntity.ok(
                errorLogRepository
                        .findByTmsFile_EntryNoOrderByTimestampDesc(entryNo)
                        .stream().map(this::toDto).toList());
    }

    @Operation(summary = "Mark an error as resolved")
    @PutMapping("/{errorId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ErrorLogDto> resolveError(
            @PathVariable Long errorId,
            @RequestBody ResolveRequest req,
            @AuthenticationPrincipal String username) {

        EdiErrorLog error = errorLogRepository.findById(errorId)
                .orElseThrow(() -> new EdiException("Error not found: " + errorId));
        error.setResolvedFlag(true);
        error.setResolvedBy(username);
        error.setResolvedAt(OffsetDateTime.now());
        error.setResolutionNote(req.getNote());
        return ResponseEntity.ok(toDto(errorLogRepository.save(error)));
    }

    private ErrorLogDto toDto(EdiErrorLog e) {
        return ErrorLogDto.builder()
                .errorId(e.getErrorId())
                .entryNo(e.getTmsFile().getEntryNo())
                .fileName(e.getTmsFile().getFileName())
                .mappingLineId(e.getMappingLineId())
                .errorType(e.getErrorType())
                .errorCode(e.getErrorCode())
                .errorMessage(e.getErrorMessage())
                .fieldPath(e.getFieldPath())
                .resolvedFlag(e.getResolvedFlag())
                .resolvedBy(e.getResolvedBy())
                .resolvedAt(e.getResolvedAt())
                .resolutionNote(e.getResolutionNote())
                .timestamp(e.getTimestamp())
                .build();
    }

    @Data public static class ResolveRequest {
        private String note;
    }
}
