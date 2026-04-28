package com.tms.edi.controller;

import com.tms.edi.dto.*;
import com.tms.edi.enums.ProcessingMode;
import com.tms.edi.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files", description = "EDI file management and processing")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class FileController {

    private final FileService             fileService;
    private final ProcessingService       processingService;
    private final StructureAnalyzerService structureAnalyzer;
    private final AuditService            auditService;

    @Operation(summary = "List all files with pagination and filtering")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<Page<FileResponseDto>> listFiles(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "50")   int size,
            @RequestParam(defaultValue = "receivedTimestamp") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        return ResponseEntity.ok(fileService.listFiles(page, size, sort, dir));
    }

    @Operation(summary = "Get file details by entry number")
    @GetMapping("/{entryNo}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<FileResponseDto> getFile(@PathVariable Long entryNo) {
        return ResponseEntity.ok(fileService.getFile(entryNo));
    }

    @Operation(summary = "Download raw file content")
    @GetMapping("/{entryNo}/download")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long entryNo,
            @AuthenticationPrincipal String username) throws Exception {

        auditService.log(username, "FILE_DOWNLOAD", "FILE", entryNo.toString(),
                "Downloaded raw file " + entryNo);

        byte[] content = fileService.downloadFile(entryNo);
        FileResponseDto meta = fileService.getFile(entryNo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getFileName() + "\"")
                .body(content);
    }

    @Operation(summary = "Get analyzed file structure tree (for mapping designer)")
    @GetMapping("/{entryNo}/structure")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<SchemaTreeDto> getStructure(@PathVariable Long entryNo) throws Exception {
        var tmsFile = fileService.getFile(entryNo);
        var entity = new com.tms.edi.entity.TmsFile();
        entity.setEntryNo(tmsFile.getEntryNo());
        entity.setFileType(tmsFile.getFileType());
        entity.setFileName(tmsFile.getFileName());
        entity.setStoragePath(tmsFile.getStoragePath());
        return ResponseEntity.ok(structureAnalyzer.analyzeFile(entity));
    }

    @Operation(summary = "Upload a file manually")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<FileResponseDto> uploadFile(
            @RequestPart("file")                        MultipartFile file,
            @RequestParam                               Long partnerId,
            @RequestParam(defaultValue = "MANUAL")      ProcessingMode processingMode,
            @AuthenticationPrincipal String username) throws Exception {

        FileResponseDto result = fileService.uploadFile(file, partnerId, processingMode);
        auditService.log(username, "FILE_UPLOAD", "FILE", result.getEntryNo().toString(),
                "Uploaded " + file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Manually trigger processing for a file")
    @PostMapping("/{entryNo}/process")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> processFile(
            @PathVariable   Long entryNo,
            @RequestBody(required = false) ProcessRequestDto req,
            @AuthenticationPrincipal String username) {

        Long mappingId = req != null ? req.getMappingId() : null;
        auditService.log(username, "MANUAL_TRIGGER", "FILE", entryNo.toString(),
                "Manual processing triggered");
        processingService.processFile(entryNo, mappingId);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Retry processing for a failed file")
    @PostMapping("/{entryNo}/retry")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<Void> retryFile(
            @PathVariable Long entryNo,
            @AuthenticationPrincipal String username) {

        auditService.log(username, "RETRY_TRIGGER", "FILE", entryNo.toString(), "Retry triggered");
        fileService.resetForRetry(entryNo);
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Soft-delete a file")
    @DeleteMapping("/{entryNo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long entryNo,
            @AuthenticationPrincipal String username) {

        auditService.log(username, "FILE_DELETE", "FILE", entryNo.toString(), "Soft-deleted");
        fileService.softDelete(entryNo);
        return ResponseEntity.noContent().build();
    }
}
