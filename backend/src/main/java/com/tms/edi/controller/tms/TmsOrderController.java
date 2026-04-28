package com.tms.edi.controller.tms;
import org.springframework.data.domain.Pageable;
import com.tms.edi.dto.tms.*;
import com.tms.edi.entity.tms.*;
import com.tms.edi.repository.tms.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for final TMS Orders (read-only after EDI processing).
 *
 * Endpoints:
 *  GET /api/v1/tms-orders             — paged list
 *  GET /api/v1/tms-orders/{orderNo}   — single order with all details
 */
@RestController
@RequestMapping("/api/v1/tms-orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TmsOrderController {

    private final TmsOrderRepository     tmsOrderRepo;
    private final TmsOrderLineRepository lineRepo;
    private final TmsOrderCargoRepository cargoRepo;

    @GetMapping
    public ResponseEntity<Page<TmsOrderDto>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long impEntryNo) {
        Pageable p = PageRequest.of(page, size, Sort.by("orderDate").descending());
        Page<TmsOrder> pageResult = impEntryNo != null
                ? tmsOrderRepo.findByImpEntryNo(impEntryNo, p)
                : tmsOrderRepo.findAllByOrderByOrderDateDesc(p);
        Page<TmsOrderDto> result = pageResult.map(this::toDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{orderNo}")
    public ResponseEntity<TmsOrderDto> getOne(@PathVariable String orderNo) {
        TmsOrder order = tmsOrderRepo.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("TMS Order not found: " + orderNo));

        TmsOrderDto dto = toDto(order);
        dto.setLines(lineRepo.findByTmsOrderId(order.getId())
                .stream().map(this::toLineDto).collect(Collectors.toList()));
        dto.setCargoItems(cargoRepo.findByTmsOrderId(order.getId())
                .stream().map(this::toCargoDto).collect(Collectors.toList()));
        dto.setReferences(order.getReferences()
                .stream().map(this::toRefDto).collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }

    // ─── Mappers ────────────────────────────────────────────────────

    private TmsOrderDto toDto(TmsOrder o) {
        return TmsOrderDto.builder()
                .id(o.getId())
                .orderNo(o.getOrderNo())
                .customerNo(o.getCustomerNo())
                .transportType(o.getTransportType())
                .tripTypeNo(o.getTripTypeNo())
                .office(o.getOffice())
                .carrierNo(o.getCarrierNo())
                .communicationPartner(o.getCommunicationPartner())
                .source(o.getSource())
                .status(o.getStatus())
                .countryOfOrigin(o.getCountryOfOrigin())
                .countryOfDestination(o.getCountryOfDestination())
                .orderDate(o.getOrderDate())
                .impEntryNo(o.getImpEntryNo())
                .importExternalOrderNo(o.getImportExternalOrderNo())
                .build();
    }

    private TmsOrderLineDto toLineDto(TmsOrderLine l) {
        return TmsOrderLineDto.builder()
                .id(l.getId())
                .lineNo(l.getLineNo())
                .actionCode(l.getActionCode())
                .addressNo(l.getAddressNo())
                .initialDatetimeFrom(l.getInitialDatetimeFrom())
                .initialDatetimeUntil(l.getInitialDatetimeUntil())
                .requestedDatetimeFrom(l.getRequestedDatetimeFrom())
                .requestedDatetimeUntil(l.getRequestedDatetimeUntil())
                .containerNo(l.getContainerNo())
                .loaded(l.getLoaded())
                .build();
    }

    private TmsOrderCargoDto toCargoDto(TmsOrderCargo c) {
        return TmsOrderCargoDto.builder()
                .id(c.getId())
                .lineNo(c.getLineNo())
                .goodNo(c.getGoodNo())
                .goodTypeCode(c.getGoodTypeCode())
                .goodSubTypeCode(c.getGoodSubTypeCode())
                .quantity(c.getQuantity())
                .unitOfMeasureCode(c.getUnitOfMeasureCode())
                .description(c.getDescription())
                .netWeight(c.getNetWeight())
                .grossWeight(c.getGrossWeight())
                .adrType(c.getAdrType())
                .dangerousGoods(c.getDangerousGoods())
                .build();
    }

    private TmsOrderReferenceDto toRefDto(TmsOrderReference r) {
        return TmsOrderReferenceDto.builder()
                .id(r.getId())
                .referenceCode(r.getReferenceCode())
                .reference(r.getReference())
                .orderLineNo(r.getOrderLineNo())
                .build();
    }
}
