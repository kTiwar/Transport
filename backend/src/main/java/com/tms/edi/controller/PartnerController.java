package com.tms.edi.controller;

import com.tms.edi.dto.PartnerDto;
import com.tms.edi.service.AuditService;
import com.tms.edi.service.PartnerService;
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
@RequestMapping("/api/v1/partners")
@Tag(name = "Partners", description = "EDI partner configuration")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PartnerController {

    private final PartnerService partnerService;
    private final AuditService   auditService;

    @Operation(summary = "List all configured partners")
    @GetMapping
    public ResponseEntity<List<PartnerDto>> listPartners() {
        return ResponseEntity.ok(partnerService.findAll());
    }

    @Operation(summary = "Get partner by ID")
    @GetMapping("/{partnerId}")
    public ResponseEntity<PartnerDto> getPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(partnerService.findById(partnerId));
    }

    @Operation(summary = "Create a new partner")
    @PostMapping
    public ResponseEntity<PartnerDto> createPartner(
            @RequestBody PartnerDto dto,
            @AuthenticationPrincipal String username) {
        PartnerDto created = partnerService.createPartner(dto);
        auditService.log(username, "PARTNER_CREATED", "PARTNER",
                created.getPartnerId().toString(), "Created " + dto.getPartnerCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update partner configuration")
    @PutMapping("/{partnerId}")
    public ResponseEntity<PartnerDto> updatePartner(
            @PathVariable Long partnerId,
            @RequestBody PartnerDto dto,
            @AuthenticationPrincipal String username) {
        PartnerDto updated = partnerService.updatePartner(partnerId, dto);
        auditService.log(username, "PARTNER_UPDATED", "PARTNER",
                partnerId.toString(), "Updated " + dto.getPartnerCode());
        return ResponseEntity.ok(updated);
    }
}
