package com.tms.edi.service.imp;

import com.tms.edi.dto.imp.ImportProcessResult;
import com.tms.edi.entity.cfg.CommunicationPartner;
import com.tms.edi.entity.imp.*;
import com.tms.edi.entity.tms.*;
import com.tms.edi.enums.ImportStatus;
import com.tms.edi.enums.MappingType;
import com.tms.edi.enums.TransactionType;
import com.tms.edi.repository.imp.*;
import com.tms.edi.repository.tms.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Processes validated Import Order records into final TMS entities.
 *
 * Java port of AL codeunit 71102980 "Go4IMP Order Processing".
 *
 * Processing flow (AL Processv2 equivalent):
 *  1. Run validation (Go4IMP Order-Check)
 *  2. Group non-original lines by distinct line-level external order id when present; otherwise one group.
 *  3. One TmsOrder per group (partition stored as {@code importExternalOrderNo}); same as one XML order per group.
 *  4. Lines / cargo / references routed to the TMS for their group (order-level refs & cargo → first group only when split).
 *  5. Mark ImportOrderHeader PROCESSED; {@code header.tmsOrderNo} is the primary (first) TMS order no.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class OrderProcessingService {

    private final ImportMappingService        mappingService;
    private final ImportOrderValidationService validationService;
    private final ImportOrderLineRepository   lineRepo;
    private final ImportOrderCargoRepository  cargoRepo;
    private final ImportOrderReferenceRepository refRepo;
    private final ImportOrderHeaderRepository headerRepo;
    private final ImportProcessingLogRepository logRepo;
    private final TmsOrderRepository          tmsOrderRepo;
    private final TmsOrderLineRepository      tmsLineRepo;
    private final TmsOrderCargoRepository     tmsCargoRepo;
    private final TmsAddressRepository        addressRepo;

    // ─────────────────────────────────────────────────────────────────
    // MAIN PROCESSING ENTRY POINT
    // ─────────────────────────────────────────────────────────────────

    /**
     * Process a single ImportOrderHeader.
     *
     * @return primary TMS number and all TMS numbers created/updated for this import row
     */
    public ImportProcessResult processOrder(Long entryNo) {
        ImportOrderHeader header = headerRepo.findById(entryNo)
                .orElseThrow(() -> new IllegalArgumentException("Import order not found: " + entryNo));

        header.setStatus(ImportStatus.PROCESSING);
        headerRepo.save(header);

        try {
            return processv2(header);
        } catch (Exception ex) {
            log.error("Processing failed for entry {}: {}", entryNo, ex.getMessage(), ex);
            setError(header, "Unexpected error: " + ex.getMessage());
            return ImportProcessResult.empty();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Processv2  (AL: Processv2)
    // ─────────────────────────────────────────────────────────────────

    private ImportProcessResult processv2(ImportOrderHeader header) {
        List<String> errors = validationService.performChecks(header);
        if (!errors.isEmpty()) {
            setError(header, String.join("; ", errors));
            return ImportProcessResult.empty();
        }

        List<ImportOrderLine> lines = lineRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());
        if (lines.isEmpty()) {
            setError(header, "No order lines to process.");
            return ImportProcessResult.empty();
        }
        lines.sort(Comparator.comparing(l -> l.getId().getLineNo()));

        boolean anyLineHasOwnExt = lines.stream()
                .anyMatch(l -> l.getExternalOrderNo() != null && !l.getExternalOrderNo().isBlank());
        Map<String, List<ImportOrderLine>> groups = groupLinesByOrderKey(lines, header, anyLineHasOwnExt);

        List<ImportOrderCargo> allCargo = cargoRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());
        List<ImportOrderReference> allRefs = refRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());

        TransactionType eff = header.getTransactionType();
        List<String> tmsNos = new ArrayList<>();
        int gi = 0;
        for (Map.Entry<String, List<ImportOrderLine>> e : groups.entrySet()) {
            boolean firstGroup = gi++ == 0;
            String dbPartition = toDbPartitionKey(e.getKey(), header, anyLineHasOwnExt);
            TmsOrder tmsOrder = resolveTmsForPartition(header, eff, dbPartition);
            if (tmsOrder == null) {
                return ImportProcessResult.empty();
            }
            upsertPayloadForPartition(header, tmsOrder, e.getValue(), allCargo, allRefs, dbPartition, anyLineHasOwnExt, firstGroup);
            tmsNos.add(tmsOrder.getOrderNo());
        }

        String primary = tmsNos.isEmpty() ? "" : tmsNos.get(0);
        header.setStatus(ImportStatus.PROCESSED);
        header.setTmsOrderNo(primary);
        header.setProcessedAt(LocalDateTime.now());
        headerRepo.save(header);

        log.info("Import order {} processed → TMS order(s): {}", header.getEntryNo(), tmsNos);
        return new ImportProcessResult(primary, tmsNos);
    }

    private Map<String, List<ImportOrderLine>> groupLinesByOrderKey(
            List<ImportOrderLine> lines, ImportOrderHeader header, boolean anyLineHasOwnExt) {
        Map<String, List<ImportOrderLine>> m = new LinkedHashMap<>();
        if (!anyLineHasOwnExt) {
            m.put("__SINGLE__", new ArrayList<>(lines));
            return m;
        }
        for (ImportOrderLine line : lines) {
            String le = line.getExternalOrderNo();
            String key;
            if (le != null && !le.isBlank()) {
                key = le.trim();
            } else {
                key = "__FOLLOW_HEADER__" + (header.getExternalOrderNo() != null ? header.getExternalOrderNo().trim() : "");
            }
            m.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
        }
        return m;
    }

    private String toDbPartitionKey(String groupKey, ImportOrderHeader header, boolean anyLineHasOwnExt) {
        if ("__SINGLE__".equals(groupKey)) {
            return truncatePartition(header.getExternalOrderNo());
        }
        if (groupKey != null && groupKey.startsWith("__FOLLOW_HEADER__")) {
            String tail = groupKey.substring("__FOLLOW_HEADER__".length()).trim();
            return tail.isEmpty() ? null : truncatePartition(tail);
        }
        return truncatePartition(groupKey);
    }

    private String truncatePartition(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        return t.length() > 80 ? t.substring(0, 80) : t;
    }

    private Optional<TmsOrder> findTmsForPartition(Long impEntryNo, String dbPartition) {
        if (dbPartition == null || dbPartition.isBlank()) {
            return tmsOrderRepo.findFirstByImpEntryNoAndImportExternalOrderNoIsNullOrderByIdDesc(impEntryNo);
        }
        return tmsOrderRepo.findFirstByImpEntryNoAndImportExternalOrderNoOrderByIdDesc(impEntryNo, dbPartition);
    }

    /** @return null if processing should abort (error already set on header). */
    private TmsOrder resolveTmsForPartition(ImportOrderHeader header, TransactionType eff, String dbPartition) {
        Optional<TmsOrder> existing = findTmsForPartition(header.getEntryNo(), dbPartition);
        if (existing.isPresent()) {
            return updateOrder(header, existing.get().getOrderNo());
        }
        if (eff == TransactionType.UPDATE_ORDER) {
            setError(header, "TMS order not found for partition: " + (dbPartition == null ? "(default)" : dbPartition));
            return null;
        }
        return insertOrder(header, dbPartition);
    }

    private void upsertPayloadForPartition(
            ImportOrderHeader header,
            TmsOrder tmsOrder,
            List<ImportOrderLine> groupLines,
            List<ImportOrderCargo> allCargo,
            List<ImportOrderReference> allRefs,
            String dbPartition,
            boolean anyLineHasOwnExt,
            boolean firstGroup) {
        Set<Integer> lineNos = groupLines.stream().map(l -> l.getId().getLineNo()).collect(Collectors.toSet());

        for (ImportOrderLine impLine : groupLines) {
            int tmsLineNo = impLine.getId().getLineNo();
            boolean lineExists = tmsLineRepo.findByTmsOrderIdAndLineNo(tmsOrder.getId(), tmsLineNo).isPresent();
            if (!lineExists) {
                insertLine(impLine, tmsOrder, tmsLineNo);
            } else {
                updateLine(impLine, tmsOrder, tmsLineNo);
            }
        }

        for (ImportOrderCargo cargo : allCargo) {
            if (!cargoBelongsToPartition(cargo, dbPartition, lineNos, anyLineHasOwnExt, firstGroup)) {
                continue;
            }
            boolean cargoExists = tmsCargoRepo
                    .findByTmsOrderIdAndLineNo(tmsOrder.getId(), cargo.getId().getLineNo())
                    .isPresent();
            if (!cargoExists) {
                insertCargo(cargo, tmsOrder);
            } else {
                updateCargo(cargo, tmsOrder);
            }
        }

        for (ImportOrderReference ref : allRefs) {
            if (!refBelongsToPartition(ref, lineNos, firstGroup)) {
                continue;
            }
            insertOrUpdateReference(ref, tmsOrder);
        }
    }

    private boolean partitionMatchesDb(String dbPartition, String externalValue) {
        String v = externalValue == null ? "" : externalValue.trim();
        String p = dbPartition == null ? "" : dbPartition.trim();
        return v.equals(p);
    }

    private boolean cargoBelongsToPartition(
            ImportOrderCargo c, String dbPartition, Set<Integer> lineNos, boolean anyLineHasOwnExt, boolean firstGroup) {
        if (c.getExternalOrderNo() != null && !c.getExternalOrderNo().isBlank()) {
            return partitionMatchesDb(dbPartition, c.getExternalOrderNo());
        }
        if (!anyLineHasOwnExt) {
            return true;
        }
        if (c.getOrderLineNo() != null && lineNos.contains(c.getOrderLineNo())) {
            return true;
        }
        return firstGroup && c.getOrderLineNo() == null;
    }

    private boolean refBelongsToPartition(ImportOrderReference ref, Set<Integer> lineNos, boolean firstGroup) {
        if (ref.getOrderLineNo() == null || ref.getOrderLineNo() == 0) {
            return firstGroup;
        }
        return lineNos.contains(ref.getOrderLineNo());
    }

    // ─────────────────────────────────────────────────────────────────
    // InsertOrder  (AL: InsertOrder)
    // ─────────────────────────────────────────────────────────────────

    private TmsOrder insertOrder(ImportOrderHeader h, String importExternalOrderNo) {
        String partner = h.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        TmsOrder order = TmsOrder.builder()
                .orderNo(generateOrderNo())
                .customerNo(mappingService.getMapping(partner, MappingType.CUSTOMER, h.getExternalCustomerNo()))
                .communicationPartner(partner)
                .source(Boolean.TRUE.equals(cp.getIsWebPortal()) ? "WEB_PORTAL" : "ORDER_IMPORT")
                .impEntryNo(h.getEntryNo())
                .importExternalOrderNo(importExternalOrderNo)
                .build();

        if (hasValue(h.getTransportType()))
            order.setTransportType(Boolean.TRUE.equals(cp.getTransportTypeIsInternal())
                    ? h.getTransportType()
                    : mappingService.getMapping(partner, MappingType.TRANSPORT_TYPE, h.getTransportType()));

        if (hasValue(h.getTripTypeNo()))
            order.setTripTypeNo(Boolean.TRUE.equals(cp.getTripTypeIsInternal())
                    ? h.getTripTypeNo()
                    : mappingService.getMapping(partner, MappingType.TRIP_TYPE, h.getTripTypeNo()));

        if (hasValue(h.getOffice()))
            order.setOffice(mappingService.getMapping(partner, MappingType.OFFICE, h.getOffice()));

        if (hasValue(h.getCustServResponsible()))
            order.setCustServResponsible(mappingService.getMapping(partner, MappingType.CS_RESPONSIBLE, h.getCustServResponsible()));

        if (hasValue(h.getSalesResponsible()))
            order.setSalesResponsible(mappingService.getMapping(partner, MappingType.SALES_RESPONSIBLE, h.getSalesResponsible()));

        if (hasValue(h.getWebPortalUser()))
            order.setWebPortalUser(h.getWebPortalUser());

        // Country routing (mapped, or same code when partner uses internal ISO-style country codes)
        if (hasValue(h.getCountryOfOrigin())) {
            order.setCountryOfOrigin(mapCountryCode(partner, cp, h.getCountryOfOrigin()));
        }
        if (hasValue(h.getCountryOfDestination())) {
            order.setCountryOfDestination(mapCountryCode(partner, cp, h.getCountryOfDestination()));
        }

        // Neutral shipment
        order.setNeutralShipment(h.getNeutralShipment());
        order.setNsAddName(h.getNsAddName());
        order.setNsAddStreet(h.getNsAddStreet());
        order.setNsAddCityPc(h.getNsAddCityPc());

        // Cash on delivery
        order.setCashOnDeliveryType(h.getCashOnDeliveryType());
        order.setCashOnDeliveryAmount(h.getCashOnDeliveryAmount());

        return tmsOrderRepo.save(order);
    }

    // ─────────────────────────────────────────────────────────────────
    // UpdateOrder  (AL: UpdateOrder)
    // ─────────────────────────────────────────────────────────────────

    private TmsOrder updateOrder(ImportOrderHeader h, String tmsOrderNo) {
        TmsOrder order = tmsOrderRepo.findByOrderNo(tmsOrderNo)
                .orElseThrow(() -> new IllegalStateException("TMS order not found: " + tmsOrderNo));

        String partner = h.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (hasValue(h.getTransportType()))
            order.setTransportType(Boolean.TRUE.equals(cp.getTransportTypeIsInternal())
                    ? h.getTransportType()
                    : mappingService.getMapping(partner, MappingType.TRANSPORT_TYPE, h.getTransportType()));
        if (hasValue(h.getTripTypeNo()))
            order.setTripTypeNo(Boolean.TRUE.equals(cp.getTripTypeIsInternal())
                    ? h.getTripTypeNo()
                    : mappingService.getMapping(partner, MappingType.TRIP_TYPE, h.getTripTypeNo()));
        if (hasValue(h.getOffice()))
            order.setOffice(mappingService.getMapping(partner, MappingType.OFFICE, h.getOffice()));
        if (hasValue(h.getCustServResponsible()))
            order.setCustServResponsible(mappingService.getMapping(partner, MappingType.CS_RESPONSIBLE, h.getCustServResponsible()));
        if (hasValue(h.getWebPortalUser()))
            order.setWebPortalUser(h.getWebPortalUser());

        order.setNeutralShipment(h.getNeutralShipment());
        order.setNsAddName(h.getNsAddName());
        order.setNsAddStreet(h.getNsAddStreet());
        order.setNsAddCityPc(h.getNsAddCityPc());
        order.setCashOnDeliveryType(h.getCashOnDeliveryType());
        order.setCashOnDeliveryAmount(h.getCashOnDeliveryAmount());
        order.setCommunicationPartner(partner);
        order.setSource(Boolean.TRUE.equals(cp.getIsWebPortal()) ? "WEB_PORTAL" : "ORDER_IMPORT");

        if (hasValue(h.getCountryOfOrigin())) {
            order.setCountryOfOrigin(mapCountryCode(partner, cp, h.getCountryOfOrigin()));
        }
        if (hasValue(h.getCountryOfDestination())) {
            order.setCountryOfDestination(mapCountryCode(partner, cp, h.getCountryOfDestination()));
        }

        return tmsOrderRepo.save(order);
    }

    /** Mapped TMS country code, or the import value when {@code countryCodeIsInternal} is true. */
    private String mapCountryCode(String partner, CommunicationPartner cp, String importCode) {
        String mapped = mappingService.getMapping(partner, MappingType.COUNTRY, importCode);
        if (!mapped.isBlank()) {
            return mapped;
        }
        if (Boolean.TRUE.equals(cp.getCountryCodeIsInternal())) {
            return importCode.strip();
        }
        return "";
    }

    // ─────────────────────────────────────────────────────────────────
    // InsertLine  (AL: InsertLine)
    // ─────────────────────────────────────────────────────────────────

    private void insertLine(ImportOrderLine impLine, TmsOrder tmsOrder, int tmsStopLineNo) {
        String partner = impLine.getCommunicationPartner();

        TmsOrderLine line = TmsOrderLine.builder()
                .tmsOrder(tmsOrder)
                .lineNo(tmsStopLineNo)
                .sortingKey(impLine.getId().getLineNo())
                .source("IMP_ORD")
                .actionCode(mappingService.getMapping(partner, MappingType.ACTION, impLine.getActionCode()))
                .addressNo(mappingService.getMapping(partner, MappingType.ADDRESS, impLine.getExternalAddressNo()))
                .initialDatetimeFrom(impLine.getInitialDatetimeFrom())
                .initialDatetimeUntil(impLine.getInitialDatetimeUntil())
                .requestedDatetimeFrom(impLine.getRequestedDatetimeFrom())
                .requestedDatetimeUntil(impLine.getRequestedDatetimeUntil())
                .bookedDatetimeFrom(impLine.getBookedDatetimeFrom())
                .bookedDatetimeUntil(impLine.getBookedDatetimeUntil())
                .closingDatetime(impLine.getClosingDatetime())
                .orderLineRef1(impLine.getOrderLineRef1())
                .orderLineRef2(impLine.getOrderLineRef2())
                .loaded(impLine.getLoaded())
                .build();

        tmsLineRepo.save(line);

        // Keep local line ID back on import line (AL: precLine."Local Order Line Id" := OrderLine."Order Line ID")
        impLine.setLocalOrderLineId(line.getId());
        lineRepo.save(impLine);
    }

    // ─────────────────────────────────────────────────────────────────
    // UpdateLine  (AL: UpdateLine)
    // ─────────────────────────────────────────────────────────────────

    private void updateLine(ImportOrderLine impLine, TmsOrder tmsOrder, int tmsStopLineNo) {
        String partner = impLine.getCommunicationPartner();

        TmsOrderLine line = tmsLineRepo
                .findByTmsOrderIdAndLineNo(tmsOrder.getId(), tmsStopLineNo)
                .orElseThrow(() -> new IllegalStateException(
                        "TMS Order Line not found for order " + tmsOrder.getOrderNo() + " stop line " + tmsStopLineNo));

        line.setActionCode(mappingService.getMapping(partner, MappingType.ACTION, impLine.getActionCode()));
        line.setAddressNo(mappingService.getMapping(partner, MappingType.ADDRESS, impLine.getExternalAddressNo()));
        line.setInitialDatetimeFrom(impLine.getInitialDatetimeFrom());
        line.setInitialDatetimeUntil(impLine.getInitialDatetimeUntil());
        line.setRequestedDatetimeFrom(impLine.getRequestedDatetimeFrom());
        line.setRequestedDatetimeUntil(impLine.getRequestedDatetimeUntil());
        line.setBookedDatetimeFrom(impLine.getBookedDatetimeFrom());
        line.setBookedDatetimeUntil(impLine.getBookedDatetimeUntil());
        line.setClosingDatetime(impLine.getClosingDatetime());
        line.setOrderLineRef1(impLine.getOrderLineRef1());
        line.setOrderLineRef2(impLine.getOrderLineRef2());
        line.setLoaded(impLine.getLoaded());
        line.setSource("IMP_ORD");

        tmsLineRepo.save(line);

        impLine.setLocalOrderLineId(line.getId());
        lineRepo.save(impLine);
    }

    // ─────────────────────────────────────────────────────────────────
    // InsertCargo  (AL: InsertCargo)
    // ─────────────────────────────────────────────────────────────────

    private void insertCargo(ImportOrderCargo impCargo, TmsOrder tmsOrder) {
        if (impCargo.getId().getLineNo() == null || impCargo.getId().getLineNo() == 0) return;

        String partner = impCargo.getCommunicationPartner();

        String desc = impCargo.getDescription() != null
                ? impCargo.getDescription().strip()
                : null;
        if (desc != null && desc.length() > 50) desc = desc.substring(0, 50);

        TmsOrderCargo cargo = TmsOrderCargo.builder()
                .tmsOrder(tmsOrder)
                .lineNo(impCargo.getId().getLineNo())
                .goodNo(mappingService.getMapping(partner, MappingType.GOOD_NUMBER, impCargo.getExternalGoodNo()))
                .goodTypeCode(mappingService.getMapping(partner, MappingType.GOOD_TYPE, impCargo.getExternalGoodType()))
                .goodSubTypeCode(mappingService.getMapping(partner, MappingType.GOOD_SUB_TYPE, impCargo.getExternalGoodSubType()))
                .quantity(impCargo.getQuantity())
                .unitOfMeasureCode(mappingService.getMapping(partner, MappingType.UNIT_OF_MEASURE, impCargo.getUnitOfMeasureCode()))
                .qtyPerUom(impCargo.getQuantity())
                .description(desc)
                .description2(impCargo.getDescription2())
                .adrType(hasValue(impCargo.getAdrType()) ? impCargo.getAdrType() : null)
                .dangerousGoods(impCargo.getDangerousGoods())
                .adrDangerousForEnvironment(impCargo.getAdrDangerousForEnvironment())
                .setTemperature(impCargo.getSetTemperature())
                .temperature(impCargo.getTemperature())
                .minTemperature(impCargo.getMinTemperature())
                .maxTemperature(impCargo.getMaxTemperature())
                .adrUnNo(impCargo.getAdrUnNo())
                .hazardClass(impCargo.getAdrHazardClass())
                .packingGroup(impCargo.getAdrPackingGroup())
                .tunnelRestrictionCode(impCargo.getAdrTunnelRestrictionCode())
                .tracingNo1(impCargo.getTracingNo1())
                .tracingNo2(impCargo.getTracingNo2())
                .netWeight(impCargo.getNetWeight())
                .grossWeight(impCargo.getGrossWeight())
                .width(impCargo.getWidth())
                .length(impCargo.getLength())
                .height(impCargo.getHeight())
                .diameter(impCargo.getDiameter())
                .palletPlaces(Boolean.TRUE.equals(impCargo.getForceLoadingMeters()) ? impCargo.getPalletPlaces() : impCargo.getPalletPlaces())
                .loadingMeters(Boolean.TRUE.equals(impCargo.getForceLoadingMeters()) ? impCargo.getLoadingMeters() : null)
                .build();

        tmsCargoRepo.save(cargo);
    }

    // ─────────────────────────────────────────────────────────────────
    // UpdateCargo  (AL: UpdateCargo)
    // ─────────────────────────────────────────────────────────────────

    private void updateCargo(ImportOrderCargo impCargo, TmsOrder tmsOrder) {
        if (impCargo.getId().getLineNo() == null || impCargo.getId().getLineNo() == 0) return;

        String partner = impCargo.getCommunicationPartner();

        TmsOrderCargo cargo = tmsCargoRepo
                .findByTmsOrderIdAndLineNo(tmsOrder.getId(), impCargo.getId().getLineNo())
                .orElseThrow(() -> new IllegalStateException(
                        "TMS Order Cargo not found for order " + tmsOrder.getOrderNo() + " line " + impCargo.getId().getLineNo()));

        cargo.setGoodNo(mappingService.getMapping(partner, MappingType.GOOD_NUMBER, impCargo.getExternalGoodNo()));
        cargo.setQuantity(impCargo.getQuantity());
        if (hasValue(impCargo.getUnitOfMeasureCode())) {
            cargo.setUnitOfMeasureCode(mappingService.getMapping(partner, MappingType.UNIT_OF_MEASURE, impCargo.getUnitOfMeasureCode()));
            cargo.setQtyPerUom(impCargo.getQuantity());
        }
        if (hasValue(impCargo.getDescription()))
            cargo.setDescription(impCargo.getDescription().strip());
        if (hasValue(impCargo.getDescription2()))
            cargo.setDescription2(impCargo.getDescription2());

        cargo.setGoodTypeCode(mappingService.getMapping(partner, MappingType.GOOD_TYPE, impCargo.getExternalGoodType()));
        cargo.setGoodSubTypeCode(mappingService.getMapping(partner, MappingType.GOOD_SUB_TYPE, impCargo.getExternalGoodSubType()));
        cargo.setAdrType(impCargo.getAdrType());
        cargo.setDangerousGoods(impCargo.getDangerousGoods());
        cargo.setSetTemperature(impCargo.getSetTemperature());
        cargo.setTemperature(impCargo.getTemperature());
        cargo.setMinTemperature(impCargo.getMinTemperature());
        cargo.setMaxTemperature(impCargo.getMaxTemperature());
        cargo.setAdrUnNo(impCargo.getAdrUnNo());
        cargo.setHazardClass(impCargo.getAdrHazardClass());
        cargo.setPackingGroup(impCargo.getAdrPackingGroup());
        cargo.setTunnelRestrictionCode(impCargo.getAdrTunnelRestrictionCode());
        cargo.setTracingNo1(impCargo.getTracingNo1());
        cargo.setTracingNo2(impCargo.getTracingNo2());
        cargo.setNetWeight(impCargo.getNetWeight());
        cargo.setGrossWeight(impCargo.getGrossWeight());
        cargo.setWidth(impCargo.getWidth());
        cargo.setLength(impCargo.getLength());
        cargo.setHeight(impCargo.getHeight());
        cargo.setDiameter(impCargo.getDiameter());
        if (impCargo.getPalletPlaces() != null) cargo.setPalletPlaces(impCargo.getPalletPlaces());
        if (Boolean.TRUE.equals(impCargo.getForceLoadingMeters()) && impCargo.getLoadingMeters() != null)
            cargo.setLoadingMeters(impCargo.getLoadingMeters());

        tmsCargoRepo.save(cargo);
    }

    // ─────────────────────────────────────────────────────────────────
    // InsertOrUpdateReference  (AL: InsertReference + UpdateReference)
    // ─────────────────────────────────────────────────────────────────

    private void insertOrUpdateReference(ImportOrderReference impRef, TmsOrder tmsOrder) {
        String partner = impRef.getCommunicationPartner();
        String mappedRefCode = mappingService.getMapping(partner, MappingType.REFERENCE_CODE, impRef.getReferenceCode());

        if (mappedRefCode.isBlank()) return;

        boolean orderLevelRef = impRef.getOrderLineNo() == null || impRef.getOrderLineNo() == 0;

        Optional<TmsOrderReference> existing = tmsOrder.getReferences().stream()
                .filter(r -> mappedRefCode.equals(r.getReferenceCode()) &&
                        (orderLevelRef ? r.getOrderLineNo() == 0 : r.getOrderLineNo().equals(impRef.getOrderLineNo())))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().setReference(impRef.getReference());
        } else {
            TmsOrderReference ref = TmsOrderReference.builder()
                    .tmsOrder(tmsOrder)
                    .referenceCode(mappedRefCode)
                    .reference(impRef.getReference())
                    .customerNo(tmsOrder.getCustomerNo())
                    .orderLineNo(orderLevelRef ? 0 : impRef.getOrderLineNo())
                    .build();
            tmsOrder.getReferences().add(ref);
        }
        tmsOrderRepo.save(tmsOrder);
    }

    // ─────────────────────────────────────────────────────────────────
    // EXTERNAL ORDER LOOKUP  (AL: IsExternalOrderExistEV)
    // ─────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────
    // ERROR HANDLING  (AL: SetError)
    // ─────────────────────────────────────────────────────────────────

    private void setError(ImportOrderHeader header, String message) {
        header.setStatus(ImportStatus.ERROR);
        header.setErrorMessage(message);
        headerRepo.save(header);

        logRepo.save(ImportProcessingLog.builder()
                .entryNo(header.getEntryNo())
                .logType("ERROR")
                .message(message)
                .build());

        log.error("Import order {} error: {}", header.getEntryNo(), message);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private String generateOrderNo() {
        // In production, use a sequence or DB-generated value
        return "ORD-" + System.currentTimeMillis() % 1_000_000;
    }
}
