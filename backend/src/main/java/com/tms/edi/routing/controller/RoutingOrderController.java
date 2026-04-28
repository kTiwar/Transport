package com.tms.edi.routing.controller;

import com.tms.edi.routing.dto.RoutingDeliveryOrderRequest;
import com.tms.edi.routing.dto.RoutingDeliveryOrderResponse;
import com.tms.edi.routing.service.ImportOrderLinePlanningService;
import com.tms.edi.routing.service.RoutingDeliveryOrderService;
import com.tms.edi.routing.service.TmsOrderRoutingImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Route-planning orders (standalone or imported from TMS order cards).
 */
@RestController
@RequestMapping("/api/v1/routing/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoutingOrderController {

    private final RoutingDeliveryOrderService orderService;
    private final TmsOrderRoutingImportService tmsImportService;
    private final ImportOrderLinePlanningService importLinePlanningService;

    @PostMapping
    public ResponseEntity<RoutingDeliveryOrderResponse> create(@Valid @RequestBody RoutingDeliveryOrderRequest req) {
        return ResponseEntity.ok(orderService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<RoutingDeliveryOrderResponse>> list() {
        return ResponseEntity.ok(orderService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoutingDeliveryOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.get(id));
    }

    /**
     * Import pickup/delivery from existing TMS order (line 1 = pickup address, line 2 = delivery address).
     */
    @PostMapping("/from-tms/{orderNo}")
    public ResponseEntity<RoutingDeliveryOrderResponse> fromTms(@PathVariable String orderNo) {
        return ResponseEntity.ok(tmsImportService.importFromTmsOrder(orderNo));
    }

    /**
     * Convert all lines of a staging Import Order into routing delivery orders.
     * Lines are paired LOAD→UNLOAD (by action code) or adjacent pairs as fallback.
     * Returns list of created routing orders ready for OR-Tools optimization.
     */
    @PostMapping("/from-import/{entryNo}")
    public ResponseEntity<List<RoutingDeliveryOrderResponse>> fromImportOrder(@PathVariable Long entryNo) {
        return ResponseEntity.ok(importLinePlanningService.importLinesAsRoutingOrders(entryNo));
    }
}
