package com.tms.edi.routing.service;

import com.tms.edi.routing.dto.RoutingDeliveryOrderRequest;
import com.tms.edi.routing.dto.RoutingDeliveryOrderResponse;
import com.tms.edi.routing.entity.RoutingDeliveryOrder;
import com.tms.edi.routing.repository.RoutingDeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoutingDeliveryOrderService {

    private final RoutingDeliveryOrderRepository orderRepo;

    @Transactional
    public RoutingDeliveryOrderResponse create(RoutingDeliveryOrderRequest req) {
        RoutingDeliveryOrder o = RoutingDeliveryOrder.builder()
                .publicOrderId(req.getPublicOrderId())
                .pickupAddress(req.getPickupAddress())
                .pickupPostcode(req.getPickupPostcode())
                .deliveryAddress(req.getDeliveryAddress())
                .deliveryPostcode(req.getDeliveryPostcode())
                .weightKg(req.getWeightKg())
                .volumeM3(req.getVolumeM3())
                .timeWindowStart(req.getTimeWindowStart())
                .timeWindowEnd(req.getTimeWindowEnd())
                .status("NEW")
                .build();
        return toDto(orderRepo.save(o));
    }

    @Transactional(readOnly = true)
    public List<RoutingDeliveryOrderResponse> list() {
        return orderRepo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoutingDeliveryOrderResponse get(Long id) {
        return orderRepo.findById(id).map(this::toDto).orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private RoutingDeliveryOrderResponse toDto(RoutingDeliveryOrder o) {
        return RoutingDeliveryOrderResponse.builder()
                .id(o.getId())
                .publicOrderId(o.getPublicOrderId())
                .pickupAddress(o.getPickupAddress())
                .pickupPostcode(o.getPickupPostcode())
                .deliveryAddress(o.getDeliveryAddress())
                .deliveryPostcode(o.getDeliveryPostcode())
                .weightKg(o.getWeightKg())
                .volumeM3(o.getVolumeM3())
                .timeWindowStart(o.getTimeWindowStart())
                .timeWindowEnd(o.getTimeWindowEnd())
                .pickupLocationId(o.getPickupLocation() != null ? o.getPickupLocation().getId() : null)
                .deliveryLocationId(o.getDeliveryLocation() != null ? o.getDeliveryLocation().getId() : null)
                .tmsOrderId(o.getTmsOrderId())
                .tmsOrderNo(o.getTmsOrderNo())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
