package com.tms.edi.routing.service;

import com.tms.edi.entity.imp.ImportOrderHeader;
import com.tms.edi.entity.imp.ImportOrderLine;
import com.tms.edi.repository.imp.ImportOrderHeaderRepository;
import com.tms.edi.repository.imp.ImportOrderLineRepository;
import com.tms.edi.routing.dto.RoutingDeliveryOrderResponse;
import com.tms.edi.routing.entity.RoutingDeliveryOrder;
import com.tms.edi.routing.repository.RoutingDeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Converts Import Order Lines (staging) into RoutingDeliveryOrders so they can
 * be fed into the OR-Tools VRP optimizer.
 *
 * Pairing strategy:
 *   Each consecutive pair of lines is treated as one pickup to delivery job:
 *     line N (action LOAD/PICKUP/LOAD) to pickup address
 *     line N+1 (action UNLOAD/DELIVERY) to delivery address
 *   If lines cannot be paired by action code, adjacent lines are paired by sequence.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportOrderLinePlanningService {

    private final ImportOrderHeaderRepository headerRepo;
    private final ImportOrderLineRepository lineRepo;
    private final RoutingDeliveryOrderRepository routingOrderRepo;

    @Transactional
    public List<RoutingDeliveryOrderResponse> importLinesAsRoutingOrders(Long entryNo) {
        ImportOrderHeader header = headerRepo.findById(entryNo)
                .orElseThrow(() -> new IllegalArgumentException("Import order not found: " + entryNo));

        List<ImportOrderLine> lines = lineRepo.findByIdEntryNo(entryNo).stream()
                .filter(l -> !Boolean.TRUE.equals(l.getOriginal()))
                .sorted(Comparator.comparingInt(l -> l.getId().getLineNo()))
                .toList();

        if (lines.isEmpty()) {
            throw new IllegalStateException("Import order " + entryNo + " has no order lines.");
        }

        // Try pairing LOAD/PICKUP to UNLOAD/DELIVERY; fall back to adjacent pairing
        List<int[]> pairs = buildPairs(lines);

        List<RoutingDeliveryOrderResponse> created = new ArrayList<>();
        for (int[] pair : pairs) {
            ImportOrderLine pickup = lines.get(pair[0]);
            ImportOrderLine delivery = lines.get(pair[1]);

            String pubId = "IMP-" + entryNo + "-L" + pickup.getId().getLineNo()
                    + "-" + delivery.getId().getLineNo();

            // Skip if already imported
            if (routingOrderRepo.findByPublicOrderId(pubId).isPresent()) {
                log.info("Routing order for pair {} already exists - skipping.", pubId);
                continue;
            }

            String pickupAddr = formatAddress(pickup);
            String deliveryAddr = formatAddress(delivery);

            if (pickupAddr.isBlank() || deliveryAddr.isBlank()) {
                log.warn("Skipping pair {}: missing address data on lines {}/{}",
                        pubId, pickup.getId().getLineNo(), delivery.getId().getLineNo());
                continue;
            }

            RoutingDeliveryOrder order = RoutingDeliveryOrder.builder()
                    .publicOrderId(pubId)
                    .pickupAddress(pickupAddr)
                    .pickupPostcode(pickup.getAddressPostalCode())
                    .deliveryAddress(deliveryAddr)
                    .deliveryPostcode(delivery.getAddressPostalCode())
                    .weightKg(1.0)
                    .volumeM3(0.01)
                    .timeWindowStart(pickup.getInitialDatetimeFrom() != null
                            ? pickup.getInitialDatetimeFrom().atOffset(ZoneOffset.UTC) : null)
                    .timeWindowEnd(delivery.getInitialDatetimeUntil() != null
                            ? delivery.getInitialDatetimeUntil().atOffset(ZoneOffset.UTC) : null)
                    .tmsOrderNo(header.getExternalOrderNo())
                    .status("NEW")
                    .build();

            RoutingDeliveryOrder saved = routingOrderRepo.save(order);
            log.info("Created routing order {} from import order {} lines {}/{}",
                    saved.getId(), entryNo, pickup.getId().getLineNo(), delivery.getId().getLineNo());
            created.add(toDto(saved));
        }

        if (created.isEmpty()) {
            throw new IllegalStateException(
                    "No new routing orders could be created from import order " + entryNo +
                    ". Check that order lines have address data and were not already imported.");
        }
        return created;
    }

    private List<int[]> buildPairs(List<ImportOrderLine> lines) {
        // Try semantic pairing: LOAD/PICKUP to UNLOAD/DELIVERY
        List<int[]> pairs = new ArrayList<>();
        List<Integer> pickupIdxs = new ArrayList<>();
        List<Integer> deliveryIdxs = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String code = lines.get(i).getActionCode();
            if (code != null) {
                String upper = code.toUpperCase();
                if (upper.contains("LOAD") || upper.contains("PICKUP") || upper.contains("COLLECT")) {
                    pickupIdxs.add(i);
                } else if (upper.contains("UNLOAD") || upper.contains("DELIVERY") || upper.contains("DELIVER")) {
                    deliveryIdxs.add(i);
                }
            }
        }

        int matched = Math.min(pickupIdxs.size(), deliveryIdxs.size());
        for (int i = 0; i < matched; i++) {
            pairs.add(new int[]{pickupIdxs.get(i), deliveryIdxs.get(i)});
        }

        // Fallback: adjacent pairing if semantic pairing produced nothing
        if (pairs.isEmpty()) {
            for (int i = 0; i + 1 < lines.size(); i += 2) {
                pairs.add(new int[]{i, i + 1});
            }
            // If odd number of lines, last line becomes a single-stop pair (same as pickup and delivery)
            if (lines.size() % 2 != 0) {
                int last = lines.size() - 1;
                pairs.add(new int[]{last, last});
            }
        }

        return pairs;
    }

    private static String formatAddress(ImportOrderLine l) {
        return Stream.of(
                        l.getAddressName(),
                        joinParts(l.getAddressStreet(), l.getAddressNumber()),
                        l.getAddressPostalCode(),
                        l.getAddressCity(),
                        l.getAddressCountryCode())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static String joinParts(String street, String number) {
        if (street == null) return number;
        if (number == null) return street;
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
                .tmsOrderNo(o.getTmsOrderNo())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }
}
