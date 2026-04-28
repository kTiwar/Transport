package com.tms.edi.service.imp;

import com.tms.edi.dto.imp.*;
import com.tms.edi.entity.imp.*;
import com.tms.edi.entity.tms.TmsOrder;
import com.tms.edi.enums.MappingType;
import com.tms.edi.enums.ImportStatus;
import com.tms.edi.enums.TransactionType;
import com.tms.edi.repository.imp.*;
import com.tms.edi.repository.tms.TmsOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Facade service for Import Order CRUD and processing trigger.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ImportOrderService {

    private final ImportOrderHeaderRepository headerRepo;
    private final ImportOrderLineRepository   lineRepo;
    private final ImportOrderCargoRepository  cargoRepo;
    private final ImportOrderReferenceRepository refRepo;
    private final ImportOrderEquipmentRepository equipRepo;
    private final ImportOrderCustomFieldRepository customFieldRepo;
    private final ImportOrderRemarkRepository remarkRepo;
    private final ImportProcessingLogRepository logRepo;
    private final OrderProcessingService      processingService;
    private final ImportOrderValidationService validationService;
    private final ImportMappingService mappingService;
    private final TmsOrderRepository tmsOrderRepo;

    // ─────────────────────────────────────────────────────────────────
    // RECEIVE / STORE
    // ─────────────────────────────────────────────────────────────────

    /** Save a new EDI Import Order in staging */
    public ImportOrderHeaderDto receiveOrder(ImportOrderHeaderDto dto) {
        ImportOrderHeader header = toEntity(dto);
        header.setStatus(ImportStatus.RECEIVED);
        header = headerRepo.save(header);

        // Persist child records
        final Long entryNo = header.getEntryNo();
        final String comm = header.getCommunicationPartner();
        final String extOrder = header.getExternalOrderNo();
        if (dto.getLines() != null) {
            List<ImportOrderLine> lines = dto.getLines().stream()
                    .map(l -> toLineEntity(l, entryNo, comm, extOrder))
                    .collect(Collectors.toList());
            lineRepo.saveAll(lines);
        }
        if (dto.getCargoItems() != null) {
            List<ImportOrderCargo> cargo = dto.getCargoItems().stream()
                    .map(c -> toCargoEntity(c, entryNo, comm, extOrder))
                    .collect(Collectors.toList());
            cargoRepo.saveAll(cargo);
        }
        if (dto.getReferences() != null) {
            List<ImportOrderReference> refs = dto.getReferences().stream()
                    .map(r -> toRefEntity(r, entryNo, comm, extOrder))
                    .collect(Collectors.toList());
            refRepo.saveAll(refs);
        }
        if (dto.getOrderEquipments() != null) {
            List<ImportOrderEquipment> equipments = dto.getOrderEquipments().stream()
                    .map(e -> toEquipmentEntity(e, entryNo, comm, extOrder))
                    .collect(Collectors.toList());
            equipRepo.saveAll(equipments);
        }

        log.info("Import order stored: entryNo={}, partner={}", header.getEntryNo(), header.getCommunicationPartner());
        return getByEntryNo(header.getEntryNo());
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ImportOrderHeaderDto> list(Pageable pageable) {
        return headerRepo.findAllByOrderByReceivedAtDesc(pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public ImportOrderHeaderDto getByEntryNo(Long entryNo) {
        ImportOrderHeader h = headerRepo.findById(entryNo)
                .orElseThrow(() -> new IllegalArgumentException("Import order not found: " + entryNo));
        ImportOrderHeaderDto dto = toDto(h);
        attachTmsAndFileLinks(entryNo, h, dto);
        dto.setLines(lineRepo.findByIdEntryNo(entryNo).stream().map(this::toLineDto).collect(Collectors.toList()));
        dto.setCargoItems(cargoRepo.findByIdEntryNo(entryNo).stream().map(this::toCargoDto).collect(Collectors.toList()));
        dto.setReferences(refRepo.findByIdEntryNo(entryNo).stream().map(this::toRefDto).collect(Collectors.toList()));
        dto.setOrderEquipments(equipRepo.findByIdEntryNo(entryNo).stream().map(this::toEquipmentDto).collect(Collectors.toList()));
        dto.setOrderCustomFields(customFieldRepo.findByEntryNoOrderByLineNoAscFieldNameAsc(entryNo).stream()
                .map(this::toCustomFieldDto)
                .collect(Collectors.toList()));
        dto.setOrderRemarks(remarkRepo.findByEntryNoOrderByLineNoAscIdAsc(entryNo).stream()
                .map(this::toRemarkDto)
                .collect(Collectors.toList()));
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────
    // PROCESSING TRIGGER
    // ─────────────────────────────────────────────────────────────────

    /** Manually trigger processing of a single import order */
    public ImportProcessResult processOrder(Long entryNo) {
        return processingService.processOrder(entryNo);
    }

    /** Validate without processing */
    @Transactional(readOnly = true)
    public List<String> validateOrder(Long entryNo) {
        ImportOrderHeader h = headerRepo.findById(entryNo)
                .orElseThrow(() -> new IllegalArgumentException("Import order not found: " + entryNo));
        log.info("Validating import order: entryNo={}, partner={}, transportType='{}'",
                entryNo, h.getCommunicationPartner(), h.getTransportType());
        return validationService.performChecks(h);
    }

    @Transactional(readOnly = true)
    public List<String> getMappedTransportTypes(String communicationPartner) {
        if (communicationPartner == null || communicationPartner.isBlank()) {
            return List.of();
        }
        return mappingService.getMappedForeignIds(communicationPartner.strip(), MappingType.TRANSPORT_TYPE);
    }

    /** Bulk process all RECEIVED orders */
    public int processPending() {
        List<ImportOrderHeader> pending = headerRepo.findByStatus(ImportStatus.RECEIVED);
        int processed = 0;
        for (ImportOrderHeader h : pending) {
            ImportProcessResult result = processingService.processOrder(h.getEntryNo());
            if (result.success()) processed++;
        }
        return processed;
    }

    /** Processing log for an import order */
    @Transactional(readOnly = true)
    public List<ImportProcessingLog> getLogs(Long entryNo) {
        return logRepo.findByEntryNoOrderByCreatedAtDesc(entryNo);
    }

    // ─────────────────────────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getStats() {
        return java.util.Map.of(
                "received",   headerRepo.countByStatus(ImportStatus.RECEIVED),
                "processing", headerRepo.countByStatus(ImportStatus.PROCESSING),
                "processed",  headerRepo.countByStatus(ImportStatus.PROCESSED),
                "error",      headerRepo.countByStatus(ImportStatus.ERROR)
        );
    }

    // ─────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────

    private ImportOrderHeader toEntity(ImportOrderHeaderDto d) {
        return ImportOrderHeader.builder()
                .communicationPartner(d.getCommunicationPartner())
                .externalOrderNo(d.getExternalOrderNo())
                .externalCustomerNo(d.getExternalCustomerNo())
                .transactionType(d.getTransactionType() != null ? d.getTransactionType() : TransactionType.INSERT_ORDER)
                .transportType(truncate(d.getTransportType(), 30))
                .tripTypeNo(d.getTripTypeNo())
                .office(d.getOffice())
                .carrierNo(d.getCarrierNo())
                .countryOfOrigin(d.getCountryOfOrigin())
                .countryOfDestination(d.getCountryOfDestination())
                .carrierName(d.getCarrierName())
                .vesselNameImport(d.getVesselNameImport())
                .vesselNameExport(d.getVesselNameExport())
                .originInfo(d.getOriginInfo())
                .destinationInfo(d.getDestinationInfo())
                .sealNo(d.getSealNo())
                .vesselEta(d.getVesselEta())
                .vesselEtd(d.getVesselEtd())
                .originPortName(d.getOriginPortName())
                .destinationPortName(d.getDestinationPortName())
                .containerNumber(d.getContainerNumber())
                .containerType(d.getContainerType())
                .containerTypeIsoCode(d.getContainerTypeIsoCode())
                .carrierId(d.getCarrierId())
                .sealNumber(d.getSealNumber())
                .importOrExport(d.getImportOrExport())
                .pickupPincode(d.getPickupPincode())
                .pickupReference(d.getPickupReference())
                .dropoffPincode(d.getDropoffPincode())
                .dropoffReference(d.getDropoffReference())
                .containerCancelled(d.getContainerCancelled())
                .vesselName(d.getVesselName())
                .closingDateTime(d.getClosingDateTime())
                .depotOutFromDateTime(d.getDepotOutFromDateTime())
                .depotInFromDateTime(d.getDepotInFromDateTime())
                .vgmClosingDateTime(d.getVgmClosingDateTime())
                .vgmWeight(d.getVgmWeight())
                .originCountry(d.getOriginCountry())
                .destinationCountry(d.getDestinationCountry())
                .orderDate(d.getOrderDate())
                .importFileEntryNo(d.getImportFileEntryNo())
                .build();
    }

    private void attachTmsAndFileLinks(Long entryNo, ImportOrderHeader h, ImportOrderHeaderDto dto) {
        List<TmsOrder> tmsRows = tmsOrderRepo.findAllByImpEntryNoOrderByIdAsc(entryNo);
        dto.setTmsOrdersForThisEntry(tmsRows.stream()
                .map(t -> ImportTmsLinkDto.builder()
                        .importEntryNo(entryNo)
                        .externalOrderNo(h.getExternalOrderNo())
                        .tmsOrderNo(t.getOrderNo())
                        .tmsPartitionExternalOrderNo(t.getImportExternalOrderNo())
                        .build())
                .collect(Collectors.toList()));
        if (h.getImportFileEntryNo() != null) {
            dto.setImportsFromSameFile(headerRepo.findByImportFileEntryNoOrderByEntryNoAsc(h.getImportFileEntryNo()).stream()
                    .map(r -> ImportTmsLinkDto.builder()
                            .importEntryNo(r.getEntryNo())
                            .externalOrderNo(r.getExternalOrderNo())
                            .tmsOrderNo(r.getTmsOrderNo())
                            .tmsPartitionExternalOrderNo(null)
                            .build())
                    .collect(Collectors.toList()));
        } else {
            dto.setImportsFromSameFile(List.of());
        }
    }

    private ImportOrderHeaderDto toDto(ImportOrderHeader h) {
        return ImportOrderHeaderDto.builder()
                .entryNo(h.getEntryNo())
                .communicationPartner(h.getCommunicationPartner())
                .externalOrderNo(h.getExternalOrderNo())
                .externalCustomerNo(h.getExternalCustomerNo())
                .transactionType(h.getTransactionType())
                .status(h.getStatus())
                .tmsOrderNo(h.getTmsOrderNo())
                .transportType(h.getTransportType())
                .tripTypeNo(h.getTripTypeNo())
                .office(h.getOffice())
                .carrierNo(h.getCarrierNo())
                .countryOfOrigin(h.getCountryOfOrigin())
                .countryOfDestination(h.getCountryOfDestination())
                .carrierName(h.getCarrierName())
                .vesselNameImport(h.getVesselNameImport())
                .vesselNameExport(h.getVesselNameExport())
                .originInfo(h.getOriginInfo())
                .destinationInfo(h.getDestinationInfo())
                .sealNo(h.getSealNo())
                .vesselEta(h.getVesselEta())
                .vesselEtd(h.getVesselEtd())
                .originPortName(h.getOriginPortName())
                .destinationPortName(h.getDestinationPortName())
                .containerNumber(h.getContainerNumber())
                .containerType(h.getContainerType())
                .containerTypeIsoCode(h.getContainerTypeIsoCode())
                .carrierId(h.getCarrierId())
                .sealNumber(h.getSealNumber())
                .importOrExport(h.getImportOrExport())
                .pickupPincode(h.getPickupPincode())
                .pickupReference(h.getPickupReference())
                .dropoffPincode(h.getDropoffPincode())
                .dropoffReference(h.getDropoffReference())
                .containerCancelled(h.getContainerCancelled())
                .vesselName(h.getVesselName())
                .closingDateTime(h.getClosingDateTime())
                .depotOutFromDateTime(h.getDepotOutFromDateTime())
                .depotInFromDateTime(h.getDepotInFromDateTime())
                .vgmClosingDateTime(h.getVgmClosingDateTime())
                .vgmWeight(h.getVgmWeight())
                .originCountry(h.getOriginCountry())
                .destinationCountry(h.getDestinationCountry())
                .orderDate(h.getOrderDate())
                .receivedAt(h.getReceivedAt())
                .processedAt(h.getProcessedAt())
                .errorMessage(h.getErrorMessage())
                .importFileEntryNo(h.getImportFileEntryNo())
                .build();
    }

    private ImportOrderLine toLineEntity(ImportOrderLineDto d, Long entryNo, String communicationPartner, String externalOrderNo) {
        String lineExt = d.getExternalOrderNo();
        String eo = (lineExt != null && !lineExt.isBlank()) ? lineExt : externalOrderNo;
        return ImportOrderLine.builder()
                .id(new ImportOrderLineId(entryNo, d.getLineNo()))
                .communicationPartner(truncate(communicationPartner, 20))
                .externalOrderNo(truncate(eo, 80))
                .actionCode(d.getActionCode())
                .externalAddressNo(d.getExternalAddressNo())
                .addressName(d.getAddressName())
                .addressCity(d.getAddressCity())
                .addressCountryCode(d.getAddressCountryCode())
                .addressPostalCode(d.getAddressPostalCode())
                .initialDatetimeFrom(d.getInitialDatetimeFrom())
                .initialDatetimeUntil(d.getInitialDatetimeUntil())
                .requestedDatetimeFrom(d.getRequestedDatetimeFrom())
                .requestedDatetimeUntil(d.getRequestedDatetimeUntil())
                .orderLineRef1(d.getOrderLineRef1())
                .orderLineRef2(d.getOrderLineRef2())
                .containerNo(d.getContainerNo())
                .loaded(d.getLoaded())
                .build();
    }

    private ImportOrderLineDto toLineDto(ImportOrderLine l) {
        return ImportOrderLineDto.builder()
                .entryNo(l.getId().getEntryNo())
                .lineNo(l.getId().getLineNo())
                .externalOrderNo(l.getExternalOrderNo())
                .actionCode(l.getActionCode())
                .externalAddressNo(l.getExternalAddressNo())
                .addressName(l.getAddressName())
                .addressCity(l.getAddressCity())
                .addressCountryCode(l.getAddressCountryCode())
                .addressPostalCode(l.getAddressPostalCode())
                .initialDatetimeFrom(l.getInitialDatetimeFrom())
                .initialDatetimeUntil(l.getInitialDatetimeUntil())
                .requestedDatetimeFrom(l.getRequestedDatetimeFrom())
                .requestedDatetimeUntil(l.getRequestedDatetimeUntil())
                .orderLineRef1(l.getOrderLineRef1())
                .orderLineRef2(l.getOrderLineRef2())
                .containerNo(l.getContainerNo())
                .loaded(l.getLoaded())
                .build();
    }

    private ImportOrderCargo toCargoEntity(ImportOrderCargoDto d, Long entryNo, String communicationPartner, String externalOrderNo) {
        String ce = d.getExternalOrderNo();
        String eo = (ce != null && !ce.isBlank()) ? ce : externalOrderNo;
        return ImportOrderCargo.builder()
                .id(new ImportOrderCargoId(entryNo, d.getLineNo()))
                .orderLineNo(d.getOrderLineNo())
                .communicationPartner(truncate(communicationPartner, 20))
                .externalOrderNo(truncate(eo, 80))
                .externalGoodNo(d.getExternalGoodNo())
                .externalGoodType(d.getExternalGoodType())
                .externalGoodSubType(d.getExternalGoodSubType())
                .quantity(d.getQuantity())
                .unitOfMeasureCode(d.getUnitOfMeasureCode())
                .description(d.getDescription())
                .netWeight(d.getNetWeight())
                .grossWeight(d.getGrossWeight())
                .adrType(d.getAdrType())
                .dangerousGoods(d.getDangerousGoods())
                .adrUnNo(d.getAdrUnNo())
                .adrHazardClass(d.getAdrHazardClass())
                .build();
    }

    private ImportOrderCargoDto toCargoDto(ImportOrderCargo c) {
        return ImportOrderCargoDto.builder()
                .entryNo(c.getId().getEntryNo())
                .lineNo(c.getId().getLineNo())
                .orderLineNo(c.getOrderLineNo())
                .externalOrderNo(c.getExternalOrderNo())
                .communicationPartner(c.getCommunicationPartner())
                .externalGoodNo(c.getExternalGoodNo())
                .externalGoodType(c.getExternalGoodType())
                .externalGoodSubType(c.getExternalGoodSubType())
                .quantity(c.getQuantity())
                .unitOfMeasureCode(c.getUnitOfMeasureCode())
                .description(c.getDescription())
                .description2(c.getDescription2())
                .netWeight(c.getNetWeight())
                .grossWeight(c.getGrossWeight())
                .width(c.getWidth())
                .length(c.getLength())
                .height(c.getHeight())
                .diameter(c.getDiameter())
                .palletPlaces(c.getPalletPlaces())
                .loadingMeters(c.getLoadingMeters())
                .forceLoadingMeters(c.getForceLoadingMeters())
                .adrType(c.getAdrType())
                .dangerousGoods(c.getDangerousGoods())
                .adrDangerousForEnvironment(c.getAdrDangerousForEnvironment())
                .adrUnNo(c.getAdrUnNo())
                .adrHazardClass(c.getAdrHazardClass())
                .adrPackingGroup(c.getAdrPackingGroup())
                .adrTunnelRestrictionCode(c.getAdrTunnelRestrictionCode())
                .tracingNo1(c.getTracingNo1())
                .tracingNo2(c.getTracingNo2())
                .temperature(c.getTemperature())
                .minTemperature(c.getMinTemperature())
                .maxTemperature(c.getMaxTemperature())
                .build();
    }

    private ImportOrderCustomFieldDto toCustomFieldDto(ImportOrderCustomField r) {
        return ImportOrderCustomFieldDto.builder()
                .id(r.getId())
                .entryNo(r.getEntryNo())
                .lineNo(r.getLineNo())
                .fieldName(r.getFieldName())
                .fieldValue(r.getFieldValue())
                .externalOrderNo(r.getExternalOrderNo())
                .communicationPartner(r.getCommunicationPartner())
                .createdBy(r.getCreatedBy())
                .creationDatetime(r.getCreationDatetime())
                .lastModifiedBy(r.getLastModifiedBy())
                .lastModificationDatetime(r.getLastModificationDatetime())
                .build();
    }

    private ImportOrderRemarkDto toRemarkDto(ImportOrderRemark r) {
        return ImportOrderRemarkDto.builder()
                .id(r.getId())
                .entryNo(r.getEntryNo())
                .externalOrderNo(r.getExternalOrderNo())
                .remarkType(r.getRemarkType())
                .lineNo(r.getLineNo())
                .remarks(r.getRemarks())
                .externalRemarkCode(r.getExternalRemarkCode())
                .importDatetime(r.getImportDatetime())
                .processedDatetime(r.getProcessedDatetime())
                .communicationPartner(r.getCommunicationPartner())
                .externalOrderLineId(r.getExternalOrderLineId())
                .orderLineNo(r.getOrderLineNo())
                .createdBy(r.getCreatedBy())
                .creationDatetime(r.getCreationDatetime())
                .lastModifiedBy(r.getLastModifiedBy())
                .lastModificationDatetime(r.getLastModificationDatetime())
                .build();
    }

    private ImportOrderReference toRefEntity(ImportOrderReferenceDto d, Long entryNo, String communicationPartner, String externalOrderNo) {
        return ImportOrderReference.builder()
                .id(new ImportOrderRefId(entryNo, d.getLineNo()))
                .communicationPartner(truncate(communicationPartner, 20))
                .externalOrderNo(truncate(externalOrderNo, 80))
                .referenceCode(d.getReferenceCode())
                .reference(d.getReference())
                .orderLineNo(d.getOrderLineNo() != null ? d.getOrderLineNo() : 0)
                .build();
    }

    private ImportOrderReferenceDto toRefDto(ImportOrderReference r) {
        return ImportOrderReferenceDto.builder()
                .entryNo(r.getId().getEntryNo())
                .lineNo(r.getId().getLineNo())
                .referenceCode(r.getReferenceCode())
                .reference(r.getReference())
                .orderLineNo(r.getOrderLineNo())
                .build();
    }

    private ImportOrderEquipment toEquipmentEntity(ImportOrderEquipmentDto d, Long entryNo, String communicationPartner, String externalOrderNo) {
        return ImportOrderEquipment.builder()
                .id(new ImportOrderEquipmentId(entryNo, d.getLineNo()))
                .communicationPartner(truncate(communicationPartner, 20))
                .externalOrderNo(truncate(externalOrderNo, 30))
                .equipmentTypeNo(truncate(d.getEquipmentTypeNo(), 50))
                .equipmentSubTypeNo(truncate(d.getEquipmentSubTypeNo(), 50))
                .materialType(truncate(d.getMaterialType() != null ? d.getMaterialType() : "-", 20))
                .quantity(d.getQuantity())
                .build();
    }

    private ImportOrderEquipmentDto toEquipmentDto(ImportOrderEquipment e) {
        return ImportOrderEquipmentDto.builder()
                .entryNo(e.getId().getEntryNo())
                .lineNo(e.getId().getLineNo())
                .externalOrderNo(e.getExternalOrderNo())
                .materialType(e.getMaterialType())
                .equipmentTypeNo(e.getEquipmentTypeNo())
                .equipmentSubTypeNo(e.getEquipmentSubTypeNo())
                .quantity(e.getQuantity())
                .communicationPartner(e.getCommunicationPartner())
                .build();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
