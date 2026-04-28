package com.tms.edi.service;

import com.tms.edi.canonical.CanonicalOrder;
import com.tms.edi.entity.*;
import com.tms.edi.enums.FileStatus;
import com.tms.edi.exception.EdiException;
import com.tms.edi.kafka.FileEventProducer;
import com.tms.edi.parser.FileParser;
import com.tms.edi.parser.ParserRegistry;
import com.tms.edi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

    private final TmsFileRepository       tmsFileRepository;
    private final MappingHeaderRepository mappingHeaderRepository;
    private final MappingService          mappingService;
    private final ParserRegistry          parserRegistry;
    private final ValidationService validationService;
    private final MandatoryCanonicalDefaultsService mandatoryDefaultsService;
    private final FileStorageService      fileStorageService;
    private final EdiOrderHeaderRepository orderHeaderRepository;
    private final EdiOrderLineRepository  orderLineRepository;
    private final EdiCargoDetailsRepository cargoRepository;
    private final EdiCostRepository       costRepository;
    private final EdiErrorLogRepository   errorLogRepository;
    private final FileEventProducer               fileEventProducer;
    private final EdiImportOrderChainingService   ediImportOrderChainingService;

    /**
     * Main processing entry point. Can be called from:
     *  - Kafka consumer (AUTO mode)
     *  - REST controller (MANUAL mode)
     *  - Quartz job (SCHEDULED mode)
     */
    @Transactional
    public void processFile(Long entryNo, Long mappingId) {
        TmsFile tmsFile = tmsFileRepository.findByEntryNoAndIsDeletedFalse(entryNo)
                .orElseThrow(() -> new EdiException("File not found: " + entryNo));

        if (tmsFile.getStatus() == FileStatus.PROCESSING) {
            log.warn("File {} is already PROCESSING — concurrent or stuck job", entryNo);
            throw new EdiException(
                    "This file is already being processed. Wait for it to finish, or click Retry on the file "
                            + "detail page to reset a stuck PROCESSING status.",
                    HttpStatus.CONFLICT);
        }

        tmsFileRepository.updateStatus(entryNo, FileStatus.PROCESSING);
        List<EdiErrorLog> errors = new ArrayList<>();

        try {
            // 1. Load raw content
            byte[] content = loadContent(tmsFile);

            // 2. Parse file into records
            FileParser parser = parserRegistry.requireParser(tmsFile.getFileType());
            List<Map<String, Object>> records;
            try (ByteArrayInputStream stream = new ByteArrayInputStream(content)) {
                records = parser.parse(stream, tmsFile.getFileName());
            }
            log.info("Parsed {} records from file {}", records.size(), entryNo);

            // 3. Resolve mapping
            MappingHeader mapping = resolveMapping(tmsFile, mappingId);

            // 4. Process each record
            int successCount = 0;
            for (int ri = 0; ri < records.size(); ri++) {
                Map<String, Object> record = records.get(ri);
                List<EdiErrorLog> recordErrors = new ArrayList<>();
                CanonicalOrder canonical = mappingService.applyMapping(mapping, record, tmsFile, recordErrors);
                mandatoryDefaultsService.applyHeaderDefaultsIfMissing(canonical, tmsFile, ri);

                if (!recordErrors.isEmpty()) {
                    errors.addAll(recordErrors);
                    continue;
                }

                boolean valid = validationService.validate(canonical, tmsFile, recordErrors, ri);
                if (!valid) {
                    errors.addAll(recordErrors);
                    continue;
                }

                insertStagingData(canonical, tmsFile);
                ediImportOrderChainingService.pushFromEdi(canonical, tmsFile, record);
                successCount++;
            }

            // 5. Finalize status
            if (!errors.isEmpty()) {
                errorLogRepository.saveAll(errors);
            }

            FileStatus finalStatus = errors.isEmpty() ? FileStatus.PROCESSED : FileStatus.ERROR;
            tmsFile.setStatus(finalStatus);
            tmsFile.setProcessedTimestamp(OffsetDateTime.now());
            tmsFile.setOrderCount(successCount);
            if (!errors.isEmpty()) {
                tmsFile.setErrorMessage(errors.get(0).getErrorMessage());
            }
            tmsFileRepository.save(tmsFile);

            fileEventProducer.publishProcessed(entryNo, finalStatus.name());
            log.info("File {} processed: {} records OK, {} errors", entryNo, successCount, errors.size());

        } catch (Exception ex) {
            log.error("Fatal processing error for file {}: {}", entryNo, ex.getMessage(), ex);
            tmsFile.setStatus(FileStatus.ERROR);
            tmsFile.setErrorMessage(ex.getMessage());
            tmsFile.setRetryCount(tmsFile.getRetryCount() + 1);
            tmsFileRepository.save(tmsFile);

            EdiErrorLog fatalError = EdiErrorLog.builder()
                    .tmsFile(tmsFile)
                    .errorType(com.tms.edi.enums.ErrorType.SCHEMA_PARSE_ERROR)
                    .errorCode("EDI-999")
                    .errorMessage("Fatal: " + ex.getMessage())
                    .build();
            errorLogRepository.save(fatalError);
            fileEventProducer.publishError(entryNo, ex.getMessage());
        }
    }

    private void insertStagingData(CanonicalOrder canonical, TmsFile tmsFile) {
        EdiPartner partner = tmsFile.getPartner();

        // Order Header
        EdiOrderHeader header = EdiOrderHeader.builder()
                .tmsFile(tmsFile)
                .partner(partner)
                .externalOrderId(canonical.getExternalOrderId())
                .customerCode(canonical.getCustomerCode())
                .orderDate(canonical.getOrderDate())
                .requestedDeliveryDate(canonical.getRequestedDeliveryDate())
                .originAddress(canonical.getOriginAddress())
                .destinationAddress(canonical.getDestinationAddress())
                .incoterm(canonical.getIncoterm())
                .priority(canonical.getPriority() != null ? canonical.getPriority() : "NORMAL")
                .build();
        orderHeaderRepository.save(header);

        // Order Lines
        if (canonical.getLines() != null) {
            List<EdiOrderLine> lines = new ArrayList<>();
            for (int i = 0; i < canonical.getLines().size(); i++) {
                var cl = canonical.getLines().get(i);
                lines.add(EdiOrderLine.builder()
                        .orderHeader(header)
                        .tmsFile(tmsFile)
                        .lineNumber(i + 1)
                        .itemCode(cl.getItemCode())
                        .description(cl.getDescription())
                        .quantity(cl.getQuantity())
                        .unitOfMeasure(cl.getUnitOfMeasure() != null ? cl.getUnitOfMeasure() : "KG")
                        .weightKg(cl.getWeightKg())
                        .volumeM3(cl.getVolumeM3())
                        .build());
            }
            orderLineRepository.saveAll(lines);
        }

        // Cargo
        if (canonical.getCargo() != null) {
            var c = canonical.getCargo();
            cargoRepository.save(EdiCargoDetails.builder()
                    .tmsFile(tmsFile)
                    .cargoType(c.getCargoType())
                    .totalWeight(c.getTotalWeight())
                    .totalVolume(c.getTotalVolume())
                    .palletCount(c.getPalletCount())
                    .hazmatFlag(c.getHazmatFlag() != null ? c.getHazmatFlag() : false)
                    .temperatureReq(c.getTemperatureReq())
                    .specialInstructions(c.getSpecialInstructions())
                    .build());
        }

        // Costs
        if (canonical.getCosts() != null) {
            List<EdiCost> costs = canonical.getCosts().stream()
                    .map(cc -> EdiCost.builder()
                            .tmsFile(tmsFile)
                            .chargeType(cc.getChargeType())
                            .amount(cc.getAmount())
                            .currency(cc.getCurrency() != null ? cc.getCurrency() : "EUR")
                            .vatAmount(cc.getVatAmount())
                            .externalChargeCode(cc.getExternalChargeCode())
                            .build())
                    .toList();
            costRepository.saveAll(costs);
        }
    }

    private MappingHeader resolveMapping(TmsFile tmsFile, Long explicitMappingId) {
        if (explicitMappingId != null) {
            MappingHeader header = mappingHeaderRepository.findById(explicitMappingId)
                    .orElseThrow(() -> new EdiException("Mapping not found: " + explicitMappingId));
            if (!header.getPartner().getPartnerId().equals(tmsFile.getPartner().getPartnerId())
                    || header.getFileType() != tmsFile.getFileType()) {
                throw new EdiException(
                        "Mapping " + explicitMappingId + " does not match this file's partner or file type");
            }
            return header;
        }
        return mappingHeaderRepository
                .findByPartner_PartnerIdAndFileTypeAndActiveFlagTrue(
                        tmsFile.getPartner().getPartnerId(), tmsFile.getFileType())
                .orElseThrow(() -> new EdiException(
                        "No active mapping found for partner " + tmsFile.getPartner().getPartnerCode()
                        + " and file type " + tmsFile.getFileType()
                        + ". Save your lines, then click Activate on the mapping."));
    }

    private byte[] loadContent(TmsFile tmsFile) throws Exception {
        if (tmsFile.getFileContent() != null) return tmsFile.getFileContent();
        return fileStorageService.load(tmsFile.getStoragePath());
    }
}
