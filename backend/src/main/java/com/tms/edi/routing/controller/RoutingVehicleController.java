package com.tms.edi.routing.controller;

import com.tms.edi.routing.dto.RoutingVehicleRequest;
import com.tms.edi.routing.dto.RoutingVehicleResponse;
import com.tms.edi.routing.service.RoutingVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/routing/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RoutingVehicleController {

    private final RoutingVehicleService vehicleService;

    @PostMapping
    public ResponseEntity<RoutingVehicleResponse> create(@Valid @RequestBody RoutingVehicleRequest req) {
        return ResponseEntity.ok(vehicleService.create(req));
    }

    @GetMapping
    public ResponseEntity<List<RoutingVehicleResponse>> list() {
        return ResponseEntity.ok(vehicleService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoutingVehicleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.get(id));
    }
}
