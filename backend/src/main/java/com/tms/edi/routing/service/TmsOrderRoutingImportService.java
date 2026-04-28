package com.tms.edi.routing.service;

import com.tms.edi.entity.tms.TmsAddress;
import com.tms.edi.entity.tms.TmsOrder;
import com.tms.edi.entity.tms.TmsOrderCargo;
import com.tms.edi.entity.tms.TmsOrderLine;
import com.tms.edi.repository.tms.TmsAddressRepository;
import com.tms.edi.repository.tms.TmsOrderCargoRepository;
import com.tms.edi.repository.tms.TmsOrderLineRepository;
import com.tms.edi.repository.tms.TmsOrderRepository;
import com.tms.edi.routing.dto.RoutingDeliveryOrderResponse;
import com.tms.edi.routing.entity.RoutingDeliveryOrder;
import com.tms.edi.routing.repository.RoutingDeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds a {@link RoutingDeliveryOrder} from an existing TMS order card:
 * first order line → pickup address, second line → delivery address (by {@code addressNo} → {@code tms_address}).
 */
@Service
@RequiredArgsConstructor
public class TmsOrderRoutingImportService {

    private final TmsOrderRepository tmsOrderRepository;
    private final TmsOrderLineRepository tmsOrderLineRepository;
    private final TmsAddressRepository tmsAddressRepository;
    private final TmsOrderCargoRepository tmsOrderCargoRepository;
    private final RoutingDeliveryOrderRepository routingDeliveryOrderRepository;

    @Transactional
    public RoutingDeliveryOrderResponse importFromTmsOrder(String orderNo) {
        TmsOrder tms = tmsOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("TMS order not found: " + orderNo));

        List<TmsOrderLine> lines = tmsOrderLineRepository.findByTmsOrderId(tms.getId()).stream()
                .sorted(Comparator.comparing(TmsOrderLine::getLineNo))
                .toList();
        if (lines.size() < 2) {
            throw new IllegalStateException("TMS order " + orderNo + " needs at least two lines (pickup + delivery) for routing import.");
        }

        TmsOrderLine pickupLine = lines.get(0);
        TmsOrderLine deliveryLine = lines.get(1);
        if (pickupLine.getAddressNo() == null || deliveryLine.getAddressNo() == null) {
            throw new IllegalStateException("Order lines must have addressNo populated for routing.");
        }

        TmsAddress pickupAddr = tmsAddressRepository.findById(pickupLine.getAddressNo())
                .orElseThrow(() -> new IllegalArgumentException("Unknown pickup address no: " + pickupLine.getAddressNo()));
        TmsAddress delAddr = tmsAddressRepository.findById(deliveryLine.getAddressNo())
                .orElseThrow(() -> new IllegalArgumentException("Unknown delivery address no: " + deliveryLine.getAddressNo()));

        double weightKg = sumCargoWeightKg(tms.getId());

        RoutingDeliveryOrder ro = RoutingDeliveryOrder.builder()
                .publicOrderId("TMS-" + tms.getOrderNo())
                .pickupAddress(formatAddress(pickupAddr))
                .pickupPostcode(nullToEmpty(pickupAddr.getPostalCode()))
                .deliveryAddress(formatAddress(delAddr))
                .deliveryPostcode(nullToEmpty(delAddr.getPostalCode()))
                .weightKg(Math.max(1.0, weightKg))
                .volumeM3(0.01)
                .timeWindowStart(pickupLine.getRequestedDatetimeFrom() != null
                        ? pickupLine.getRequestedDatetimeFrom().atOffset(java.time.ZoneOffset.UTC) : null)
                .timeWindowEnd(deliveryLine.getRequestedDatetimeUntil() != null
                        ? deliveryLine.getRequestedDatetimeUntil().atOffset(java.time.ZoneOffset.UTC) : null)
                .tmsOrderId(tms.getId())
                .tmsOrderNo(tms.getOrderNo())
                .status("NEW")
                .build();

        if (pickupAddr.getLatitude() != null && pickupAddr.getLongitude() != null) {
            // Optional: seed from TMS address master if already geocoded
            // GeocodingService will still run on optimize if locations null — here we skip DB link for brevity
        }

        RoutingDeliveryOrder saved = routingDeliveryOrderRepository.save(ro);
        return toDto(saved);
    }

    private double sumCargoWeightKg(Long tmsOrderId) {
        List<TmsOrderCargo> cargo = tmsOrderCargoRepository.findByTmsOrderId(tmsOrderId);
        BigDecimal sum = cargo.stream()
                .map(TmsOrderCargo::getGrossWeight)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.doubleValue() > 0 ? sum.doubleValue() : 1.0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String formatAddress(TmsAddress a) {
        return java.util.stream.Stream.of(
                        joinParts(a.getStreet(), a.getNumber()),
                        a.getPostalCode(),
                        a.getCity(),
                        a.getCountryCode())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static String joinParts(String street, String number) {
        if (street == null && number == null) {
            return null;
        }
        if (street == null) {
            return number;
        }
        if (number == null) {
            return street;
        }
        return street + " " + number;
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
                .tmsOrderId(o.getTmsOrderId())
                .tmsOrderNo(o.getTmsOrderNo())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
