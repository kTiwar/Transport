package com.tms.edi.controller;

import com.tms.edi.dto.MonitoringStatsDto;
import com.tms.edi.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/monitoring")
@Tag(name = "Monitoring", description = "Processing statistics and health metrics")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Operation(summary = "Get overall processing statistics")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','VIEWER')")
    public ResponseEntity<MonitoringStatsDto> getStats() {
        return ResponseEntity.ok(monitoringService.getStats());
    }
}
