package com.tms.edi.routing.controller;

import com.tms.edi.routing.dto.OptimizeRoutesRequest;
import com.tms.edi.routing.dto.RoutingRouteResponse;
import com.tms.edi.routing.service.RouteOptimizationOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routing/routes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoutingRouteController {

    private final RouteOptimizationOrchestrator orchestrator;

    @PostMapping("/optimize")
    public ResponseEntity<List<RoutingRouteResponse>> optimize(@Valid @RequestBody OptimizeRoutesRequest req) {
        return ResponseEntity.ok(orchestrator.optimize(req));
    }

    @GetMapping
    public ResponseEntity<List<RoutingRouteResponse>> list() {
        return ResponseEntity.ok(orchestrator.listRoutes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoutingRouteResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(orchestrator.getRoute(id));
    }
}
