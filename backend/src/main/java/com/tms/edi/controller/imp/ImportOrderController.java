package com.tms.edi.controller.imp;

import com.tms.edi.dto.imp.ImportOrderHeaderDto;
import com.tms.edi.dto.imp.ImportProcessResult;
import com.tms.edi.entity.imp.ImportProcessingLog;
import com.tms.edi.service.imp.ImportOrderService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for Import Order staging operations.
 *
 * Endpoints:
 *  POST   /api/v1/import-orders          — receive/store EDI order
 *  GET    /api/v1/import-orders          — list all import orders (paged)
 *  GET    /api/v1/import-orders/{entryNo} — get single import order with all child records
 *  POST   /api/v1/import-orders/{entryNo}/process  — trigger processing
 *  POST   /api/v1/import-orders/{entryNo}/validate — validate without processing
 *  POST   /api/v1/import-orders/process-all        — bulk process all RECEIVED orders
 *  GET    /api/v1/import-orders/{entryNo}/logs      — processing logs
 *  GET    /api/v1/import-orders/stats              — counts by status
 */
@RestController
@RequestMapping("/api/v1/import-orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ImportOrderController {

    private final ImportOrderService importOrderService;

    @PostMapping
    public ResponseEntity<ImportOrderHeaderDto> receiveOrder(@RequestBody ImportOrderHeaderDto dto) {
        log.info("Received import order payload: partner={}, externalOrderNo={}, transportType='{}'",
                dto.getCommunicationPartner(), dto.getExternalOrderNo(), dto.getTransportType());
        return ResponseEntity.ok(importOrderService.receiveOrder(dto));
    }

    @GetMapping
    public ResponseEntity<Page<ImportOrderHeaderDto>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(importOrderService.list(
                PageRequest.of(page, size, Sort.by("receivedAt").descending())));
    }

    @GetMapping("/{entryNo}")
    public ResponseEntity<ImportOrderHeaderDto> getOne(@PathVariable Long entryNo) {
        return ResponseEntity.ok(importOrderService.getByEntryNo(entryNo));
    }

    @PostMapping("/{entryNo}/process")
    public ResponseEntity<Map<String, Object>> processOne(@PathVariable Long entryNo) {
        ImportProcessResult r = importOrderService.processOrder(entryNo);
        if (!r.success()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Processing failed. Check logs for entry " + entryNo));
        }
        return ResponseEntity.ok(Map.of(
                "tmsOrderNo", r.primaryTmsOrderNo(),
                "tmsOrderNos", r.allTmsOrderNos(),
                "message", "Processed successfully"));
    }

    @PostMapping("/{entryNo}/validate")
    public ResponseEntity<Map<String, Object>> validateOne(@PathVariable Long entryNo) {
        List<String> errors = importOrderService.validateOrder(entryNo);
        return ResponseEntity.ok(Map.of(
                "valid",  errors.isEmpty(),
                "errors", errors
        ));
    }

    @GetMapping("/transport-types")
    public ResponseEntity<List<String>> getMappedTransportTypes(@RequestParam String communicationPartner) {
        return ResponseEntity.ok(importOrderService.getMappedTransportTypes(communicationPartner));
    }

    @PostMapping("/process-all")
    public ResponseEntity<Map<String, Integer>> processAll() {
        int count = importOrderService.processPending();
        return ResponseEntity.ok(Map.of("processedCount", count));
    }

    @GetMapping("/{entryNo}/logs")
    public ResponseEntity<List<ImportProcessingLog>> getLogs(@PathVariable Long entryNo) {
        return ResponseEntity.ok(importOrderService.getLogs(entryNo));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(importOrderService.getStats());
    }
}
