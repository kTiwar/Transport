package com.tms.edi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.edi.canonical.*;
import com.tms.edi.dto.MappingDto;
import com.tms.edi.dto.MappingLineDto;
import com.tms.edi.dto.MappingVersionHistoryDto;
import com.tms.edi.entity.*;
import com.tms.edi.enums.ErrorType;
import com.tms.edi.enums.FileType;
import com.tms.edi.exception.EdiException;
import com.tms.edi.repository.EdiPartnerRepository;
import com.tms.edi.repository.MappingHeaderRepository;
import com.tms.edi.repository.MappingLineRepository;
import com.tms.edi.repository.MappingVersionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {

    private final MappingHeaderRepository        mappingHeaderRepository;
    private final MappingLineRepository          mappingLineRepository;
    private final MappingVersionHistoryRepository versionHistoryRepository;
    private final EdiPartnerRepository           partnerRepository;
    private final TransformationService          transformationService;
    private final ObjectMapper                   objectMapper;

    // ── Retrieval ─────────────────────────────────────────────────────────────

    public MappingDto findById(Long mappingId) {
        MappingHeader header = mappingHeaderRepository.findById(mappingId)
                .orElseThrow(() -> new EdiException("Mapping not found: " + mappingId));
        return toDto(header);
    }

    public Optional<MappingHeader> findActiveMapping(Long partnerId, FileType fileType) {
        return mappingHeaderRepository
                .findByPartner_PartnerIdAndFileTypeAndActiveFlagTrue(partnerId, fileType);
    }

    public List<MappingDto> findByPartner(Long partnerId) {
        return mappingHeaderRepository.findByPartner_PartnerId(partnerId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Create / Update ───────────────────────────────────────────────────────

    @Transactional
    public MappingDto createMapping(MappingDto dto, String createdBy) {
        EdiPartner partner = partnerRepository.findById(dto.getPartnerId())
                .orElseThrow(() -> new EdiException("Partner not found: " + dto.getPartnerId()));

        Integer maxVersion = mappingHeaderRepository.findMaxVersion(dto.getPartnerId(), dto.getFileType());
        int nextVersion = maxVersion == null ? 1 : maxVersion + 1;

        MappingHeader header = MappingHeader.builder()
                .partner(partner)
                .fileType(dto.getFileType())
                .mappingName(dto.getMappingName())
                .version(nextVersion)
                .activeFlag(false)
                .description(dto.getDescription())
                .createdBy(createdBy)
                .build();

        if (dto.getLines() != null) {
            dto.getLines().forEach(lineDto -> header.getLines().add(toLineEntity(lineDto, header)));
        }

        MappingHeader saved = mappingHeaderRepository.save(header);
        log.info("Created mapping {} v{} for partner {}", dto.getMappingName(), nextVersion, partner.getPartnerCode());
        return toDto(saved);
    }

    @Transactional
    public MappingDto updateMappingLines(Long mappingId, MappingDto dto, String updatedBy) {
        MappingHeader header = mappingHeaderRepository.findById(mappingId)
                .orElseThrow(() -> new EdiException("Mapping not found: " + mappingId));

        int oldVersion = header.getVersion() != null ? header.getVersion() : 1;

        // 1. Load existing lines via a direct query — do NOT touch header.getLines()
        //    so that orphanRemoval never fires on the Hibernate-managed collection.
        List<MappingLine> existingLines = mappingLineRepository
                .findByMappingHeader_MappingIdOrderBySequenceAsc(mappingId);

        // 2. Snapshot current state for version history
        versionHistoryRepository.save(MappingVersionHistory.builder()
                .mappingId(mappingId)
                .version(oldVersion)
                .savedBy(updatedBy)
                .savedAt(OffsetDateTime.now())
                .linesSnapshot(serializeLines(existingLines))
                .changeSummary("Saved " + (dto.getLines() != null ? dto.getLines().size() : 0) + " lines")
                .build());

        // 3. Bulk-delete existing lines in a single SQL statement.
        //    deleteAllInBatch uses DELETE WHERE id IN (...) — never throws StaleStateException
        //    because it does not expect an exact per-row count.
        if (!existingLines.isEmpty()) {
            mappingLineRepository.deleteAllInBatch(existingLines);
        }

        // 4. Insert the new lines directly — bypass the header collection entirely
        List<MappingLine> newLines = new ArrayList<>();
        if (dto.getLines() != null) {
            int seq = 0;
            for (MappingLineDto lineDto : dto.getLines()) {
                String target = lineDto.getTargetField() != null ? lineDto.getTargetField().trim() : "";
                if (target.isEmpty()) {
                    throw new EdiException("Each mapping line must have a target field name");
                }
                lineDto.setTargetField(target);
                MappingLine line = toLineEntity(lineDto, header);
                line.setSequence(lineDto.getSequence() != null ? lineDto.getSequence() : seq++);
                newLines.add(line);
            }
        }
        List<MappingLine> savedLines = mappingLineRepository.saveAll(newLines);

        // 5. Update header metadata only (no collection manipulation)
        header.setVersion(oldVersion + 1);
        header.setStatus("DRAFT");
        mappingHeaderRepository.save(header);

        log.info("Saved {} mapping lines for mapping {} (now v{}) by {}",
                savedLines.size(), mappingId, header.getVersion(), updatedBy);

        // 6. Build response DTO from in-memory data (avoid lazy-load after batch ops)
        return MappingDto.builder()
                .mappingId(header.getMappingId())
                .partnerId(header.getPartner().getPartnerId())
                .partnerCode(header.getPartner().getPartnerCode())
                .fileType(header.getFileType())
                .mappingName(header.getMappingName())
                .version(header.getVersion())
                .activeFlag(header.getActiveFlag())
                .status(header.getStatus())
                .description(header.getDescription())
                .createdBy(header.getCreatedBy())
                .createdDate(header.getCreatedDate())
                .updatedDate(header.getUpdatedDate())
                .lines(savedLines.stream().map(this::toLineDto).collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<MappingVersionHistoryDto> getVersionHistory(Long mappingId) {
        mappingHeaderRepository.findById(mappingId)
                .orElseThrow(() -> new EdiException("Mapping not found: " + mappingId));
        return versionHistoryRepository.findByMappingIdOrderBySavedAtDesc(mappingId)
                .stream()
                .map(v -> {
                    int lineCount = 0;
                    if (v.getLinesSnapshot() != null) {
                        try {
                            lineCount = objectMapper.readTree(v.getLinesSnapshot()).size();
                        } catch (Exception ignored) {}
                    }
                    return MappingVersionHistoryDto.builder()
                            .id(v.getId())
                            .mappingId(v.getMappingId())
                            .version(v.getVersion())
                            .savedBy(v.getSavedBy())
                            .savedAt(v.getSavedAt())
                            .changeSummary(v.getChangeSummary())
                            .lineCount(lineCount)
                            .build();
                })
                .toList();
    }

    private String serializeLines(List<MappingLine> lines) {
        try {
            return objectMapper.writeValueAsString(
                    lines.stream().map(this::toLineDto).toList());
        } catch (Exception e) {
            return "[]";
        }
    }

    @Transactional
    public void activateMapping(Long mappingId) {
        MappingHeader header = mappingHeaderRepository.findById(mappingId)
                .orElseThrow(() -> new EdiException("Mapping not found: " + mappingId));
        mappingHeaderRepository.deactivateAllForPartnerAndType(
                header.getPartner().getPartnerId(), header.getFileType());
        header.setActiveFlag(true);
        mappingHeaderRepository.save(header);
        log.info("Activated mapping {} for partner {}", mappingId, header.getPartner().getPartnerCode());
    }

    // ── Mapping Execution ─────────────────────────────────────────────────────

    /**
     * Apply all mapping lines to a single parsed record and return a CanonicalOrder.
     */
    public CanonicalOrder applyMapping(MappingHeader mappingHeader,
                                       Map<String, Object> record,
                                       TmsFile tmsFile,
                                       List<EdiErrorLog> errorAccumulator) {

        CanonicalOrder.CanonicalOrderBuilder orderBuilder = CanonicalOrder.builder();
        CanonicalCargo.CanonicalCargoBuilder cargoBuilder = CanonicalCargo.builder();
        CanonicalTransport.CanonicalTransportBuilder transportBuilder = CanonicalTransport.builder();
        List<String> extraNotes = new ArrayList<>();

        List<MappingLine> lines = mappingLineRepository
                .findByMappingHeader_MappingIdOrderBySequenceAsc(mappingHeader.getMappingId());

        if (lines.isEmpty()) {
            log.warn("Mapping id={} name='{}' has no saved lines — header will stay empty unless XML inference applies",
                    mappingHeader.getMappingId(), mappingHeader.getMappingName());
        }

        for (MappingLine line : lines) {
            try {
                Object rawValue = resolveSourceValue(line, record);
                Map<String, Object> params = parseParams(line.getTransformationParams());
                Object transformed = transformationService.apply(
                        line.getTransformationRule(), rawValue, params);

                if (transformed == null || transformed.toString().isBlank()) {
                    if (line.getDefaultValue() != null && !line.getDefaultValue().isBlank()) {
                        transformed = line.getDefaultValue();
                    } else if (Boolean.TRUE.equals(line.getIsRequired())) {
                        errorAccumulator.add(buildError(tmsFile, line, ErrorType.MISSING_MANDATORY_FIELD,
                                "EDI-001", "Required field missing: " + line.getTargetField(),
                                line.getSourceFieldPath()));
                        continue;
                    }
                }

                if (transformed != null) {
                    applyToBuilder(line.getTargetField(), transformed,
                            orderBuilder, cargoBuilder, transportBuilder, extraNotes);
                }

            } catch (Exception ex) {
                log.error("Transformation error on field {}: {}", line.getTargetField(), ex.getMessage());
                errorAccumulator.add(buildError(tmsFile, line, ErrorType.TRANSFORMATION_FAILURE,
                        "EDI-002", ex.getMessage(), line.getSourceFieldPath()));
            }
        }

        if (!extraNotes.isEmpty()) {
            orderBuilder.notes(String.join("\n", extraNotes));
        }

        CanonicalOrder built = orderBuilder
                .cargo(cargoBuilder.build())
                .transportRequest(transportBuilder.build())
                .build();
        return enrichCanonicalFromKnownXmlPatterns(built, record);
    }

    /**
     * When mapping lines are missing or source paths do not resolve, copy common WMS XML leaf values
     * from the flattened record so validation can succeed (e.g. ExternalOrderNumber / CustomerNo).
     */
    private CanonicalOrder enrichCanonicalFromKnownXmlPatterns(CanonicalOrder order, Map<String, Object> record) {
        String ext = order.getExternalOrderId();
        String cust = order.getCustomerCode();
        boolean needExt = ext == null || ext.isBlank();
        boolean needCust = cust == null || cust.isBlank();
        boolean needDate = order.getOrderDate() == null;
        if (!needExt && !needCust && !needDate) {
            return order;
        }

        String inferredExt = null;
        String inferredCust = null;
        LocalDate inferredDate = null;
        for (Map.Entry<String, Object> e : record.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            String key = e.getKey();
            String tail = xmlPathTail(key);
            String v = e.getValue().toString().trim();
            if (v.isEmpty()) continue;
            if (needExt && inferredExt == null && "ExternalOrderNumber".equalsIgnoreCase(tail)) {
                inferredExt = v;
            }
            if (needCust && inferredCust == null && "CustomerNo".equalsIgnoreCase(tail)) {
                inferredCust = v;
            }
            if (needDate && inferredDate == null
                    && ("InitialDateTimeFrom".equalsIgnoreCase(tail) || "OrderDate".equalsIgnoreCase(tail))) {
                inferredDate = parseDate(v);
            }
            // Prefer first line datetime under this order’s OrderLines (split-record maps are order-scoped)
            if (needDate && inferredDate == null && key.contains("OrderLine") && "InitialDateTimeFrom".equalsIgnoreCase(tail)) {
                inferredDate = parseDate(v);
            }
        }
        if (needExt && inferredExt != null) {
            order.setExternalOrderId(inferredExt);
        }
        if (needCust && inferredCust != null) {
            order.setCustomerCode(inferredCust);
        }
        if (needDate && inferredDate != null) {
            order.setOrderDate(inferredDate);
        }
        return order;
    }

    private static String xmlPathTail(String path) {
        int p = path.lastIndexOf('/');
        return p < 0 ? path : path.substring(p + 1);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> parseParams(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Resolves source path against the flattened XML/JSON record (one order per map when XML was split).
     * <ul>
     *   <li>Exact key, then {@code [*]} replaced with {@code [0]}…{@code [63]}</li>
     *   <li>Single XML siblings omit indices → {@code OrderLine/Action} not {@code OrderLine[0]/Action}: try collapsed path</li>
     *   <li>Regex: each {@code [*]} may match missing bracket or {@code [n]}</li>
     *   <li>If mapping root differs from file root, match from {@code /Orders/Order/…} onward</li>
     *   <li>Unique key ending with {@code /LastSegment}</li>
     * </ul>
     */
    private Object resolveSourceValue(MappingLine line, Map<String, Object> record) {
        if (line.getSourceFieldPath() == null || line.getSourceFieldPath().isBlank()) return null;
        String path = line.getSourceFieldPath().trim();

        Object hit = tryResolveConcretePath(path, record);
        if (hit != null) return hit;

        int ord = path.indexOf("/Orders/Order");
        if (ord > 0) {
            hit = tryResolveConcretePath(path.substring(ord), record);
            if (hit != null) return hit;
        }

        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            String tail = path.substring(lastSlash + 1);
            if (!tail.isBlank() && !tail.contains("*") && !tail.contains("[")) {
                String suffix = "/" + tail;
                List<String> matches = new ArrayList<>();
                for (String key : record.keySet()) {
                    if (key.endsWith(suffix)) matches.add(key);
                }
                if (matches.size() == 1) return record.get(matches.get(0));
            }
        }

        return null;
    }

    private Object tryResolveConcretePath(String path, Map<String, Object> record) {
        Object hit = record.get(path);
        if (hit != null) return hit;

        if (path.contains("[*]")) {
            String collapsed = path.replace("[*]", "");
            hit = record.get(collapsed);
            if (hit != null) return hit;

            for (int i = 0; i < 64; i++) {
                String candidate = path.replace("[*]", "[" + i + "]");
                hit = record.get(candidate);
                if (hit != null) return hit;
            }

            hit = firstValueMatchingWildcardPath(path, record);
            if (hit != null) return hit;
        }

        return null;
    }

    /**
     * Each {@code [*]} becomes an optional {@code [digits]} segment (covers single vs repeated XML children).
     */
    private static String wildcardPathToRegex(String path) {
        String[] parts = path.split(Pattern.quote("[*]"), -1);
        if (parts.length == 1) {
            return Pattern.quote(path);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(Pattern.quote(parts[i]));
            if (i < parts.length - 1) {
                sb.append("(?:\\[\\d+\\])?");
            }
        }
        return sb.toString();
    }

    private static Object firstValueMatchingWildcardPath(String path, Map<String, Object> record) {
        List<String> regexes = new ArrayList<>();
        regexes.add("^" + wildcardPathToRegex(path) + "$");
        int ord = path.indexOf("/Orders/Order");
        if (ord >= 0) {
            regexes.add("^.*" + wildcardPathToRegex(path.substring(ord)) + "$");
        }
        for (String rx : regexes) {
            Pattern p = Pattern.compile(rx);
            List<String> keys = new ArrayList<>();
            for (String key : record.keySet()) {
                if (p.matcher(key).matches()) {
                    keys.add(key);
                }
            }
            if (!keys.isEmpty()) {
                Collections.sort(keys);
                return record.get(keys.get(0));
            }
        }
        return null;
    }

    private void applyToBuilder(String targetRaw, Object value,
                                 CanonicalOrder.CanonicalOrderBuilder order,
                                 CanonicalCargo.CanonicalCargoBuilder cargo,
                                 CanonicalTransport.CanonicalTransportBuilder transport,
                                 List<String> extraNotes) {
        if (targetRaw == null || targetRaw.isBlank()) {
            log.debug("Skipping mapping line with empty target field");
            return;
        }
        String target = targetRaw.trim();
        String v = value.toString();
        switch (target) {
            // Go4IMP / designer uses external_order_number; staging DB column is external_order_id
            case "external_order_id", "external_order_number" -> order.externalOrderId(v);
            case "customer_code", "customer_no" -> order.customerCode(v);
            // WMS / Go4IMP datetimes often yyyyMMddHHmmss on stop lines or header
            case "order_date", "creation_date_time", "initial_dt_from", "initial_dt_until" ->
                    order.orderDate(parseDate(v));
            case "requested_delivery_date", "booked_dt_from", "booked_dt_until" ->
                    order.requestedDeliveryDate(parseDate(v));
            case "incoterm"                 -> order.incoterm(v);
            case "priority"                 -> order.priority(v);
            case "origin_address"           -> order.originAddress(v);
            case "destination_address"      -> order.destinationAddress(v);
            case "communication_partner", "transaction_type", "customer_name",
                 "reference1", "reference2", "reference3" ->
                    extraNotes.add(target + ": " + v);
            case "neutral_shipment", "neutral_shipment_address_name", "neutral_shipment_address_street",
                 "neutral_shipment_address_postcode_city" ->
                    extraNotes.add(target + ": " + v);
            case "references[].reference_code", "references[].reference_value" ->
                    extraNotes.add(target + ": " + v);
            // Go4IMP cargos[] → single CanonicalCargo bucket
            case "cargo.cargo_type", "cargos[].good_no", "cargos[].cargo_no", "cargos[].good_description" ->
                    cargo.cargoType(v);
            case "cargo.total_weight", "total_gross_weight", "cargos[].gross_weight" -> {
                BigDecimal w = parseBigDecimal(v);
                if (w != null) cargo.totalWeight(w);
            }
            case "cargo.total_volume", "total_volume", "cargos[].volume" -> {
                BigDecimal vol = parseBigDecimal(v);
                if (vol != null) cargo.totalVolume(vol);
            }
            case "cargos[].pallet_places" -> {
                Integer pc = parseIntSafe(v);
                if (pc != null) cargo.palletCount(pc);
            }
            case "cargo.hazmat", "cargos[].dangerous_goods" -> cargo.hazmatFlag(parseBooleanLoose(v));
            // Stop line → free-text addresses (map LOAD/UNLOAD rows to different source paths in UI)
            case "stop_lines[].address_name", "stop_lines[].address_street", "stop_lines[].address_postal_code",
                 "stop_lines[].address_city", "stop_lines[].address_country_code" ->
                    extraNotes.add(target + ": " + v);
            case "transport.origin"         -> transport.origin(v);
            case "transport.destination"    -> transport.destination(v);
            case "transport.mode", "transport_type" -> transport.transportMode(v);
            default -> log.debug("Unmapped target field: {}", target);
        }
    }

    private static BigDecimal parseBigDecimal(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return new BigDecimal(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer parseIntSafe(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return new BigDecimal(v.trim()).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean parseBooleanLoose(String v) {
        if (v == null) return false;
        String s = v.trim();
        return Boolean.parseBoolean(s) || "1".equals(s) || "Y".equalsIgnoreCase(s) || "YES".equalsIgnoreCase(s);
    }

    /**
     * ISO-8601 date, yyyyMMdd, or WMS-style yyyyMMddHHmmss (uses local date part).
     */
    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        String s = v.trim();
        try {
            return LocalDate.parse(s);
        } catch (Exception ignored) {
        }
        try {
            if (s.length() >= 14 && s.chars().limit(14).allMatch(Character::isDigit)) {
                return LocalDateTime.parse(s.substring(0, 14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        .toLocalDate();
            }
        } catch (Exception ignored) {
        }
        try {
            if (s.length() >= 8 && s.chars().limit(8).allMatch(Character::isDigit)) {
                return LocalDate.parse(s.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private EdiErrorLog buildError(TmsFile file, MappingLine line, ErrorType type,
                                    String code, String message, String path) {
        return EdiErrorLog.builder()
                .tmsFile(file)
                .mappingLineId(line != null ? line.getMappingLineId() : null)
                .errorType(type)
                .errorCode(code)
                .errorMessage(message)
                .fieldPath(path)
                .build();
    }

    private MappingLine toLineEntity(MappingLineDto dto, MappingHeader header) {
        return MappingLine.builder()
                .mappingHeader(header)
                .sourceFieldPath(dto.getSourceFieldPath())
                .targetField(dto.getTargetField())
                .transformationRule(dto.getTransformationRule())
                .transformationParams(dto.getTransformationParams())
                .defaultValue(dto.getDefaultValue())
                .isRequired(Boolean.TRUE.equals(dto.getIsRequired()))
                .sequence(dto.getSequence() != null ? dto.getSequence() : 0)
                .conditionRule(dto.getConditionRule())
                .lookupTableName(dto.getLookupTableName())
                .build();
    }

    public MappingDto toDto(MappingHeader h) {
        return MappingDto.builder()
                .mappingId(h.getMappingId())
                .partnerId(h.getPartner().getPartnerId())
                .partnerCode(h.getPartner().getPartnerCode())
                .fileType(h.getFileType())
                .mappingName(h.getMappingName())
                .version(h.getVersion())
                .activeFlag(h.getActiveFlag())
                .status(h.getStatus())
                .description(h.getDescription())
                .createdBy(h.getCreatedBy())
                .createdDate(h.getCreatedDate())
                .updatedDate(h.getUpdatedDate())
                .lines(h.getLines().stream().map(this::toLineDto).collect(Collectors.toList()))
                .build();
    }

    private MappingLineDto toLineDto(MappingLine l) {
        return MappingLineDto.builder()
                .mappingLineId(l.getMappingLineId())
                .sourceFieldPath(l.getSourceFieldPath())
                .targetField(l.getTargetField())
                .transformationRule(l.getTransformationRule())
                .transformationParams(l.getTransformationParams())
                .defaultValue(l.getDefaultValue())
                .isRequired(l.getIsRequired())
                .sequence(l.getSequence())
                .conditionRule(l.getConditionRule())
                .lookupTableName(l.getLookupTableName())
                .build();
    }
}
