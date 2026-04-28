package com.tms.edi.service;

import com.tms.edi.canonical.CanonicalOrder;
import com.tms.edi.dto.imp.ImportOrderCargoDto;
import com.tms.edi.dto.imp.ImportOrderEquipmentDto;
import com.tms.edi.dto.imp.ImportOrderHeaderDto;
import com.tms.edi.dto.imp.ImportOrderLineDto;
import com.tms.edi.dto.imp.ImportOrderReferenceDto;
import com.tms.edi.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a Go4IMP {@link ImportOrderHeaderDto} (header + lines + cargo + references) from the
 * flattened XML/JSON record produced by {@link com.tms.edi.parser.FileParser} and the canonical order.
 */
public final class EdiFlatRecordToImportOrderMapper {

    private static final int MAX_OFFICE = 20;
    private static final int MAX_TRANSPORT = 20;
    private static final int MAX_COUNTRY = 10;
    private static final int MAX_ACTION = 20;
    private static final int MAX_ADDR_NO = 50;
    private static final int MAX_NAME = 100;
    private static final int MAX_CITY = 50;
    private static final int MAX_POSTAL = 20;
    private static final int MAX_CONTAINER = 20;
    private static final int MAX_GOOD_NO = 50;
    private static final int MAX_GOOD_TYPE = 50;
    private static final int MAX_UOM = 20;
    private static final int MAX_DESC = 50;
    private static final int MAX_REF_CODE = 20;
    private static final int MAX_REF_VAL = 100;
    private static final int MAX_LINE_REF = 50;
    private static final int MAX_EQ_TYPE = 50;
    private static final int MAX_EQ_SUB = 50;
    private static final int MAX_MATERIAL = 20;
    private static final int MAX_CARRIER_NAME = 100;
    private static final int MAX_VESSEL_NAME = 100;
    private static final int MAX_VESSEL_NAME_IMP_EXP = 80;
    private static final int MAX_CONTAINER_NO = 50;
    private static final int MAX_CONTAINER_TYPE = 20;
    private static final int MAX_ISO_CODE = 10;
    private static final int MAX_CARRIER_ID = 20;
    private static final int MAX_SEAL = 50;
    private static final int MAX_IMPORT_EXPORT = 20;
    private static final int MAX_PIN = 50;
    private static final int MAX_REF_LONG = 150;
    private static final int MAX_PORT = 50;
    private static final int MAX_INFO = 100;
    private static final int MAX_SEAL_HDR = 30;
    private static final int MAX_EXT_ORDER_LINE = 80;

    private static final Pattern ORDER_LINE_LEAF =
            Pattern.compile(".*/OrderLine(?:\\[(\\d+)])?/([^/]+)$");
    /** Header-level goods under {@code Order[/Order[n]]/OrderCargos/OrderCargo/…}. */
    private static final Pattern ORDER_HEADER_CARGO_LEAF =
            Pattern.compile(".*/Order(?:\\[(\\d+)])?/OrderCargos/OrderCargo(?:\\[(\\d+)])?/([^/]+)$");
    /**
     * Line-level goods under {@code Order[/Order[n]]/OrderLines/OrderLine/…/OrderLineCargo/…}
     * (same semantics as header cargo for import; many feeds only send goods here).
     */
    private static final Pattern ORDER_LINE_CARGO_LEAF =
            Pattern.compile(
                    ".*/Order(?:\\[(\\d+)])?/OrderLines/OrderLine(?:\\[(\\d+)])?/OrderLineCargos/OrderLineCargo(?:\\[(\\d+)])?/([^/]+)$");
    /** Header-level references under {@code Order/OrderReferences/OrderReference/…}. */
    private static final Pattern ORDER_REFERENCE_LEAF =
            Pattern.compile(".*/Order(?:\\[(\\d+)])?/OrderReferences/OrderReference(?:\\[(\\d+)])?/([^/]+)$");
    /** Line-level references under {@code …/OrderLineReferences/OrderLineReference/…}. */
    private static final Pattern ORDER_LINE_REFERENCE_LEAF =
            Pattern.compile(
                    ".*/Order(?:\\[(\\d+)])?/OrderLines/OrderLine(?:\\[(\\d+)])?/OrderLineReferences/OrderLineReference(?:\\[(\\d+)])?/([^/]+)$");
    /** Header-level equipment under {@code Order/OrderEquipments/OrderEquipment/…}. */
    private static final Pattern ORDER_EQUIPMENT_LEAF =
            Pattern.compile(".*/Order(?:\\[(\\d+)])?/OrderEquipments/OrderEquipment(?:\\[(\\d+)])?/([^/]+)$");
    /** Header-level container details under {@code …/Order[/Order[n]]/ContainerInfo/…}. */
    private static final Pattern CONTAINER_INFO_LEAF =
            Pattern.compile(".*/Order(?:\\[(\\d+)])?/ContainerInfo(?:\\[(\\d+)])?/([^/]+)$");
    /** Header-level vessel details under {@code …/Order[/Order[n]]/VesselInfo/…}. */
    private static final Pattern VESSEL_INFO_LEAF =
            Pattern.compile(".*/Order(?:\\[(\\d+)])?/VesselInfo(?:\\[(\\d+)])?/([^/]+)$");

    /** Batch XML: {@code /Orders/Order[0]/…} vs single {@code /…/Order/…}. */
    private static final Pattern BATCH_ORDER_SEGMENT = Pattern.compile("/Order\\[(\\d+)\\]/");

    private static final DateTimeFormatter COMPACT_DT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private EdiFlatRecordToImportOrderMapper() {
    }

    private static int batchOrderIndex(String flatKey) {
        Matcher m = BATCH_ORDER_SEGMENT.matcher(flatKey);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static long lineComposite(int orderBranch, int lineIdxInOrder) {
        return (long) orderBranch * 10_000L + lineIdxInOrder;
    }

    private static List<Long> sortedLineComposites(Map<String, Object> rec) {
        TreeSet<Long> set = new TreeSet<>();
        for (String key : rec.keySet()) {
            Matcher m = ORDER_LINE_LEAF.matcher(key);
            if (!m.find()) {
                continue;
            }
            int lineIdx = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            set.add(lineComposite(batchOrderIndex(key), lineIdx));
        }
        return new ArrayList<>(set);
    }

    private static int globalLineNo(Map<String, Object> rec, int orderBranch, int lineIdxInOrder) {
        long c = lineComposite(orderBranch, lineIdxInOrder);
        List<Long> order = sortedLineComposites(rec);
        int idx = order.indexOf(c);
        return idx >= 0 ? idx + 1 : 0;
    }

    /** {@code ExternalOrderNumber} for the {@code Order[n]} subtree (batch XML). */
    private static String externalOrderNumberForOrder(Map<String, Object> rec, int orderBranch) {
        String suffix = "/ExternalOrderNumber";
        for (Map.Entry<String, Object> e : rec.entrySet()) {
            String k = e.getKey();
            if (k.contains("/Order[" + orderBranch + "]/") && k.endsWith(suffix)) {
                return stringify(e.getValue());
            }
        }
        if (orderBranch == 0) {
            String v = stringify(firstValueEndingWithSegment(rec, "/Order/ExternalOrderNumber"));
            if (!v.isEmpty()) {
                return v;
            }
        }
        return stringify(firstValueEndingWithSegment(rec, "/ExternalOrderNumber"));
    }

    public static ImportOrderHeaderDto build(
            CanonicalOrder canonical,
            String communicationPartner,
            String externalOrderNo,
            String externalCustomerNo,
            Map<String, Object> flatRecord) {
        Map<String, Object> rec = flatRecord != null ? flatRecord : Map.of();

        LocalDateTime orderDate = null;
        if (canonical != null && canonical.getOrderDate() != null) {
            orderDate = canonical.getOrderDate().atStartOfDay();
        } else {
            orderDate = parseOrderDateFromFlat(rec);
        }

        String transportType = trimOrNull(canonical != null ? canonical.getIncoterm() : null);
        if (transportType == null) {
            transportType = truncate(firstValueEndingWithSegment(rec, "/TransportType"), MAX_TRANSPORT);
        }

        String office = truncate(firstValueEndingWithSegment(rec, "/Reference1"), MAX_OFFICE);

        TransactionType tx = parseTransactionType(firstValueEndingWithSegment(rec, "/TransactionType"));

        List<ImportOrderLineDto> lines = extractLines(rec);
        String countryOrigin = null;
        String countryDest = null;
        if (!lines.isEmpty()) {
            countryOrigin = truncate(lines.get(0).getAddressCountryCode(), MAX_COUNTRY);
            countryDest = truncate(lines.get(lines.size() - 1).getAddressCountryCode(), MAX_COUNTRY);
        }

        List<ImportOrderCargoDto> cargo = extractCargo(rec);
        List<ImportOrderReferenceDto> references = extractReferences(rec);
        List<ImportOrderEquipmentDto> equipments = extractEquipments(rec);

        Map<String, String> containerLeaves = extractFirstSection(rec, CONTAINER_INFO_LEAF);
        Map<String, String> vesselLeaves = extractFirstSection(rec, VESSEL_INFO_LEAF);

        String carrierName = truncate(firstOf(containerLeaves,
                "CarrierName", "carrierName", "ShippingLine", "shippingLine", "Carrier", "carrier"), MAX_CARRIER_NAME);
        if (carrierName == null && canonical != null && canonical.getTransportRequest() != null) {
            carrierName = truncate(trimOrNull(canonical.getTransportRequest().getCarrier()), MAX_CARRIER_NAME);
        }

        String vesselName = truncate(firstOf(vesselLeaves,
                "VesselName", "vesselName", "Vessel", "vessel", "ShipName", "shipName"), MAX_VESSEL_NAME);
        if (vesselName == null) {
            vesselName = truncate(firstValueEndingWithSegment(rec, "/VesselName"), MAX_VESSEL_NAME);
        }
        String vesselNameImport = truncate(firstOf(vesselLeaves,
                "VesselNameImport", "vesselNameImport", "VesselImport", "ImportVesselName"), MAX_VESSEL_NAME_IMP_EXP);
        String vesselNameExport = truncate(firstOf(vesselLeaves,
                "VesselNameExport", "vesselNameExport", "VesselExport", "ExportVesselName"), MAX_VESSEL_NAME_IMP_EXP);

        LocalDateTime vesselEta = parseDateTimeFlexible(firstOf(vesselLeaves,
                "ETA", "Eta", "eta", "VesselETA", "vesselETA", "EstimatedArrival", "estimatedArrival"));
        if (vesselEta == null) {
            vesselEta = parseDateTimeFlexible(firstValueEndingWithSegment(rec, "/ETA"));
        }
        LocalDateTime vesselEtd = parseDateTimeFlexible(firstOf(vesselLeaves,
                "ETD", "Etd", "etd", "VesselETD", "vesselETD", "EstimatedDeparture", "estimatedDeparture"));
        if (vesselEtd == null) {
            vesselEtd = parseDateTimeFlexible(firstValueEndingWithSegment(rec, "/ETD"));
        }

        String containerNum = truncate(firstOf(containerLeaves,
                "ContainerNumber", "containerNumber", "ContainerNo", "containerNo", "ContainerID"), MAX_CONTAINER_NO);
        if (containerNum == null) {
            containerNum = truncate(firstValueEndingWithSegment(rec, "/ContainerNumber"), MAX_CONTAINER_NO);
        }
        if (containerNum == null) {
            containerNum = truncate(firstValueEndingWithSegment(rec, "/ContainerNo"), MAX_CONTAINER_NO);
        }

        return ImportOrderHeaderDto.builder()
                .communicationPartner(communicationPartner)
                .externalOrderNo(externalOrderNo)
                .externalCustomerNo(externalCustomerNo)
                .transactionType(tx)
                .orderDate(orderDate != null ? orderDate : LocalDateTime.now())
                .transportType(transportType)
                .office(office)
                .countryOfOrigin(countryOrigin)
                .countryOfDestination(countryDest)
                .carrierName(carrierName)
                .vesselNameImport(vesselNameImport)
                .vesselNameExport(vesselNameExport)
                .originInfo(truncate(firstOf(vesselLeaves, "OriginInfo", "originInfo"), MAX_INFO))
                .destinationInfo(truncate(firstOf(vesselLeaves, "DestinationInfo", "destinationInfo"), MAX_INFO))
                .sealNo(truncate(firstOf(vesselLeaves, "SealNo", "sealNo", "HeaderSealNo"), MAX_SEAL_HDR))
                .vesselEta(vesselEta)
                .vesselEtd(vesselEtd)
                .originPortName(truncate(firstOf(vesselLeaves,
                        "OriginPortName", "originPortName", "OriginPort", "POL", "PortOfLoading"), MAX_PORT))
                .destinationPortName(truncate(firstOf(vesselLeaves,
                        "DestinationPortName", "destinationPortName", "DestinationPort", "POD", "PortOfDischarge"), MAX_PORT))
                .containerNumber(containerNum)
                .containerType(truncate(firstOf(containerLeaves, "ContainerType", "containerType", "Type"), MAX_CONTAINER_TYPE))
                .containerTypeIsoCode(truncate(firstOf(containerLeaves,
                        "ContainerTypeISOCode", "containerTypeISOCode", "ISOCode", "isoCode", "IsoCode"), MAX_ISO_CODE))
                .carrierId(truncate(firstOf(containerLeaves, "CarrierId", "carrierId", "CarrierID", "SCAC"), MAX_CARRIER_ID))
                .sealNumber(truncate(firstOf(containerLeaves,
                        "SealNumber", "sealNumber", "Seal", "seal", "SealNo", "sealNo"), MAX_SEAL))
                .importOrExport(truncate(firstOf(containerLeaves,
                        "ImportOrExport", "importOrExport", "Direction", "IE", "ImportExport"), MAX_IMPORT_EXPORT))
                .pickupPincode(truncate(firstOf(containerLeaves, "PickupPincode", "pickupPincode", "PickupPostalCode"), MAX_PIN))
                .pickupReference(truncate(firstOf(containerLeaves, "PickupReference", "pickupReference"), MAX_REF_LONG))
                .dropoffPincode(truncate(firstOf(containerLeaves, "DropoffPincode", "dropoffPincode", "DropoffPostalCode"), MAX_PIN))
                .dropoffReference(truncate(firstOf(containerLeaves, "DropoffReference", "dropoffReference"), MAX_REF_LONG))
                .containerCancelled(parseBooleanFlexible(firstOf(containerLeaves,
                        "ContainerCancelled", "containerCancelled", "Cancelled", "cancelled")))
                .vesselName(vesselName)
                .closingDateTime(parseDateTimeFlexible(firstOf(vesselLeaves,
                        "ClosingDateTime", "closingDateTime", "ClosingDate", "CutOff", "CutOffDateTime")))
                .depotOutFromDateTime(parseDateTimeFlexible(firstOf(vesselLeaves,
                        "DepotOutFromDateTime", "depotOutFromDateTime", "DepotOut", "depotOut")))
                .depotInFromDateTime(parseDateTimeFlexible(firstOf(vesselLeaves,
                        "DepotInFromDateTime", "depotInFromDateTime", "DepotIn", "depotIn")))
                .vgmClosingDateTime(parseDateTimeFlexible(firstOf(vesselLeaves,
                        "VGMClosingDateTime", "vgmClosingDateTime", "VgmClosingDateTime", "VGMClosing", "vgmClosing")))
                .vgmWeight(parseBigDecimal(firstOf(vesselLeaves, "VGMWeight", "vgmWeight", "VgmWeight", "vgm")))
                .originCountry(truncate(firstOf(vesselLeaves,
                        "OriginCountry", "originCountry", "OriginCountryCode", "POLCountry"), MAX_COUNTRY))
                .destinationCountry(truncate(firstOf(vesselLeaves,
                        "DestinationCountry", "destinationCountry", "DestinationCountryCode", "PODCountry"), MAX_COUNTRY))
                .lines(lines.isEmpty() ? null : lines)
                .cargoItems(cargo.isEmpty() ? null : cargo)
                .references(references.isEmpty() ? null : references)
                .orderEquipments(equipments.isEmpty() ? null : equipments)
                .build();
    }

    /**
     * First {@code ContainerInfo}/{@code VesselInfo} sibling group (lowest index) as leaf name → value.
     */
    private static Map<String, String> extractFirstSection(Map<String, Object> rec, Pattern sectionLeaf) {
        TreeMap<Integer, TreeMap<Integer, Map<String, String>>> byOrderThenSection = new TreeMap<>();
        for (Map.Entry<String, Object> e : rec.entrySet()) {
            Matcher m = sectionLeaf.matcher(e.getKey());
            if (!m.find()) {
                continue;
            }
            int orderBr = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int secIdx = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            String leaf = m.group(3);
            String val = stringify(e.getValue());
            byOrderThenSection
                    .computeIfAbsent(orderBr, k -> new TreeMap<>())
                    .computeIfAbsent(secIdx, k -> new LinkedHashMap<>())
                    .put(leaf, val);
        }
        if (byOrderThenSection.isEmpty()) {
            return Map.of();
        }
        TreeMap<Integer, Map<String, String>> sections = byOrderThenSection.firstEntry().getValue();
        return sections.isEmpty() ? Map.of() : sections.firstEntry().getValue();
    }

    private static List<ImportOrderLineDto> extractLines(Map<String, Object> rec) {
        TreeMap<Long, Map<String, String>> byComposite = new TreeMap<>();
        for (Map.Entry<String, Object> e : rec.entrySet()) {
            Matcher m = ORDER_LINE_LEAF.matcher(e.getKey());
            if (!m.find()) {
                continue;
            }
            int lineIdxInOrder = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            String leaf = m.group(2);
            int orderBr = batchOrderIndex(e.getKey());
            long composite = lineComposite(orderBr, lineIdxInOrder);
            byComposite.computeIfAbsent(composite, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
        }

        List<ImportOrderLineDto> out = new ArrayList<>();
        int displayLineNo = 1;
        for (Map.Entry<Long, Map<String, String>> ent : byComposite.entrySet()) {
            long composite = ent.getKey();
            int orderBr = (int) (composite / 10_000L);
            Map<String, String> f = ent.getValue();
            String action = firstOf(f, "Action", "action");
            String extOrder = truncate(externalOrderNumberForOrder(rec, orderBr), MAX_EXT_ORDER_LINE);
            ImportOrderLineDto line = ImportOrderLineDto.builder()
                    .lineNo(displayLineNo++)
                    .externalOrderNo(extOrder)
                    .actionCode(truncate(action, MAX_ACTION))
                    .externalAddressNo(truncate(firstOf(f, "ExternalAddressID", "ExternalAddressId", "externalAddressID"), MAX_ADDR_NO))
                    .addressName(truncate(firstOf(f, "AddressName", "addressName"), MAX_NAME))
                    .addressCity(truncate(firstOf(f, "AddressCity", "addressCity"), MAX_CITY))
                    .addressCountryCode(truncate(firstOf(f, "AddressCountryCode", "addressCountryCode"), MAX_COUNTRY))
                    .addressPostalCode(truncate(firstOf(f, "AddressPostalCode", "addressPostalCode"), MAX_POSTAL))
                    .initialDatetimeFrom(parseCompactDateTime(firstOf(f, "InitialDateTimeFrom", "initialDateTimeFrom")))
                    .initialDatetimeUntil(parseCompactDateTime(firstOf(f, "InitialDateTimeUntil", "initialDateTimeUntil")))
                    .requestedDatetimeFrom(parseCompactDateTime(firstOf(f, "RequestedDateTimeFrom", "requestedDateTimeFrom")))
                    .requestedDatetimeUntil(parseCompactDateTime(firstOf(f, "RequestedDateTimeUntil", "requestedDateTimeUntil")))
                    .containerNo(truncate(firstOf(f, "ContainerNo", "Container", "containerNo"), MAX_CONTAINER))
                    .orderLineRef1(truncate(firstOf(f, "OrderLineRef1", "orderLineRef1"), MAX_LINE_REF))
                    .orderLineRef2(truncate(firstOf(f, "OrderLineRef2", "orderLineRef2"), MAX_LINE_REF))
                    .loaded(action != null && action.equalsIgnoreCase("LOAD") ? Boolean.TRUE
                            : action != null && action.equalsIgnoreCase("UNLOAD") ? Boolean.FALSE : null)
                    .build();
            out.add(line);
        }
        return out;
    }

    /**
     * Line cargo keys are {@code >=} this value so they never collide with header cargo
     * ({@code orderBr * 1e6 + cargoIdx} reaches {@code 1_000_000} for {@code Order[1]}/first cargo).
     */
    private static final long LINE_CARGO_SORT_NAMESPACE = 5_000_000_000L;
    /** Disjoint from header cargo keys and from each other. */
    private static final long HEADER_REF_SORT_OFFSET = 500_000_000L;
    private static final long LINE_REF_SORT_OFFSET = 600_000_000L;
    private static final long ORDER_EQUIPMENT_SORT_OFFSET = 700_000_000L;

    private static List<ImportOrderCargoDto> extractCargo(Map<String, Object> rec) {
        TreeMap<Long, Map<String, String>> bySortKey = new TreeMap<>();

        for (Map.Entry<String, Object> e : rec.entrySet()) {
            String key = e.getKey();
            Matcher mh = ORDER_HEADER_CARGO_LEAF.matcher(key);
            if (mh.find()) {
                int orderBr = mh.group(1) != null ? Integer.parseInt(mh.group(1)) : 0;
                int cargoIdx = mh.group(2) != null ? Integer.parseInt(mh.group(2)) : 0;
                String leaf = mh.group(3);
                long sortKey = (long) orderBr * 1_000_000L + cargoIdx;
                bySortKey.computeIfAbsent(sortKey, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
                continue;
            }
            Matcher ml = ORDER_LINE_CARGO_LEAF.matcher(key);
            if (ml.find()) {
                int orderBr = ml.group(1) != null ? Integer.parseInt(ml.group(1)) : 0;
                int orderLineIdx = ml.group(2) != null ? Integer.parseInt(ml.group(2)) : 0;
                int lineCargoIdx = ml.group(3) != null ? Integer.parseInt(ml.group(3)) : 0;
                String leaf = ml.group(4);
                long sortKey = LINE_CARGO_SORT_NAMESPACE + (long) orderBr * 100_000L + (long) orderLineIdx * 1_000L + lineCargoIdx;
                bySortKey.computeIfAbsent(sortKey, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
            }
        }

        List<ImportOrderCargoDto> out = new ArrayList<>();
        int displayLineNo = 1;
        for (Map.Entry<Long, Map<String, String>> ent : bySortKey.entrySet()) {
            long sortKey = ent.getKey();
            Map<String, String> f = ent.getValue();
            boolean isHeader = sortKey < LINE_CARGO_SORT_NAMESPACE;
            int orderBr = isHeader ? (int) (sortKey / 1_000_000L)
                    : (int) ((sortKey - LINE_CARGO_SORT_NAMESPACE) / 100_000L);
            int orderLineIdxInOrder = isHeader ? 0
                    : (int) (((sortKey - LINE_CARGO_SORT_NAMESPACE) % 100_000L) / 1_000L);
            String extOrder = truncate(externalOrderNumberForOrder(rec, orderBr), MAX_EXT_ORDER_LINE);
            Integer orderLineNo = isHeader ? null : globalLineNo(rec, orderBr, orderLineIdxInOrder);
            ImportOrderCargoDto c = ImportOrderCargoDto.builder()
                    .lineNo(displayLineNo++)
                    .orderLineNo(orderLineNo)
                    .externalOrderNo(extOrder)
                    .externalGoodNo(truncate(firstOf(f,
                            "ExternalGoodID", "externalGoodId", "GoodNo", "goodNo"), MAX_GOOD_NO))
                    .externalGoodType(truncate(firstOf(f, "GoodType", "goodType"), MAX_GOOD_TYPE))
                    .externalGoodSubType(truncate(firstOf(f, "GoodSubType", "goodSubType"), MAX_GOOD_TYPE))
                    .quantity(parseBigDecimal(firstOf(f, "Quantity", "quantity")))
                    .unitOfMeasureCode(truncate(firstOf(f, "UnitOfMeasure", "UnitOfMeasureCode", "unitOfMeasure"), MAX_UOM))
                    .description(truncate(firstOf(f, "GoodDescription", "goodDescription", "Description"), MAX_DESC))
                    .netWeight(parseBigDecimal(firstOf(f, "NetWeight", "netWeight")))
                    .grossWeight(parseBigDecimal(firstOf(f, "GrossWeight", "grossWeight")))
                    .adrType(truncate(firstOf(f, "AdrType", "adrType", "ADRCode", "adrCode"), 20))
                    .dangerousGoods(parseBoolean01(firstOf(f, "DangerousGoods", "dangerousGoods")))
                    .adrUnNo(truncate(firstOf(f, "AdrUnNo", "adrUnNo", "ADRUNNumber", "adrUnNumber", "UNNo"), 20))
                    .adrHazardClass(truncate(firstOf(f, "AdrHazardClass", "adrHazardClass", "ADRClassification", "adrClassification"), 20))
                    .build();
            out.add(c);
        }
        return out;
    }

    private static List<ImportOrderReferenceDto> extractReferences(Map<String, Object> rec) {
        TreeMap<Long, Map<String, String>> bySortKey = new TreeMap<>();
        /** Line-level refs only: global import line_no for {@link OrderProcessingService} routing. */
        TreeMap<Long, Integer> lineRefGlobalLineNo = new TreeMap<>();

        for (Map.Entry<String, Object> e : rec.entrySet()) {
            String key = e.getKey();
            Matcher mh = ORDER_REFERENCE_LEAF.matcher(key);
            if (mh.find()) {
                int orderBr = mh.group(1) != null ? Integer.parseInt(mh.group(1)) : 0;
                int refIdx = mh.group(2) != null ? Integer.parseInt(mh.group(2)) : 0;
                String leaf = mh.group(3);
                long sortKey = HEADER_REF_SORT_OFFSET + (long) orderBr * 1_000_000L + refIdx;
                bySortKey.computeIfAbsent(sortKey, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
                continue;
            }
            Matcher ml = ORDER_LINE_REFERENCE_LEAF.matcher(key);
            if (ml.find()) {
                int orderBr = ml.group(1) != null ? Integer.parseInt(ml.group(1)) : 0;
                int orderLineIdx = ml.group(2) != null ? Integer.parseInt(ml.group(2)) : 0;
                int refIdx = ml.group(3) != null ? Integer.parseInt(ml.group(3)) : 0;
                String leaf = ml.group(4);
                long sortKey = LINE_REF_SORT_OFFSET + (long) orderBr * 100_000L + (long) orderLineIdx * 1_000L + refIdx;
                bySortKey.computeIfAbsent(sortKey, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
                int gLine = globalLineNo(rec, orderBr, orderLineIdx);
                if (gLine > 0) {
                    lineRefGlobalLineNo.put(sortKey, gLine);
                }
            }
        }

        List<ImportOrderReferenceDto> out = new ArrayList<>();
        int displayLineNo = 1;
        for (Map.Entry<Long, Map<String, String>> ent : bySortKey.entrySet()) {
            long sortKey = ent.getKey();
            Map<String, String> f = ent.getValue();
            Integer oln = lineRefGlobalLineNo.get(sortKey);
            int orderLineNoVal = oln != null ? oln : 0;
            ImportOrderReferenceDto r = ImportOrderReferenceDto.builder()
                    .lineNo(displayLineNo++)
                    .referenceCode(truncate(firstOf(f, "ReferenceCode", "referenceCode"), MAX_REF_CODE))
                    .reference(truncate(firstOf(f, "ReferenceValue", "referenceValue", "Reference"), MAX_REF_VAL))
                    .orderLineNo(orderLineNoVal)
                    .build();
            out.add(r);
        }
        return out;
    }

    private static List<ImportOrderEquipmentDto> extractEquipments(Map<String, Object> rec) {
        TreeMap<Long, Map<String, String>> bySortKey = new TreeMap<>();
        for (Map.Entry<String, Object> e : rec.entrySet()) {
            Matcher m = ORDER_EQUIPMENT_LEAF.matcher(e.getKey());
            if (!m.find()) {
                continue;
            }
            int orderBr = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
            int eqIdx = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            String leaf = m.group(3);
            long sortKey = ORDER_EQUIPMENT_SORT_OFFSET + (long) orderBr * 1_000_000L + eqIdx;
            bySortKey.computeIfAbsent(sortKey, k -> new LinkedHashMap<>()).put(leaf, stringify(e.getValue()));
        }

        List<ImportOrderEquipmentDto> out = new ArrayList<>();
        int displayLineNo = 1;
        for (Map.Entry<Long, Map<String, String>> ent : bySortKey.entrySet()) {
            Map<String, String> f = ent.getValue();
            String mat = truncate(firstOf(f, "MaterialType", "materialType", "Material", "material"), MAX_MATERIAL);
            String type = truncate(firstOf(f, "EquipmentType", "equipmentType", "EquipmentTypeNo", "equipmentTypeNo"), MAX_EQ_TYPE);
            String sub = truncate(firstOf(f, "EquipmentSubType", "equipmentSubType", "EquipmentSubTypeNo", "equipmentSubTypeNo"), MAX_EQ_SUB);
            Integer qty = parseInteger(firstOf(f, "Quantity", "quantity"));
            if ((mat == null || mat.isBlank()) && type == null && sub == null && qty == null) {
                continue;
            }
            ImportOrderEquipmentDto eq = ImportOrderEquipmentDto.builder()
                    .lineNo(displayLineNo++)
                    .materialType(mat != null && !mat.isBlank() ? mat : "-")
                    .equipmentTypeNo(type)
                    .equipmentSubTypeNo(sub)
                    .quantity(qty)
                    .build();
            out.add(eq);
        }
        return out;
    }

    private static Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String firstValueEndingWithSegment(Map<String, Object> rec, String segmentSuffix) {
        for (Map.Entry<String, Object> e : rec.entrySet()) {
            if (e.getKey().endsWith(segmentSuffix)) {
                return stringify(e.getValue());
            }
        }
        return null;
    }

    private static LocalDateTime parseOrderDateFromFlat(Map<String, Object> rec) {
        String raw = firstValueEndingWithSegment(rec, "/OrderDate");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        raw = raw.trim();
        try {
            if (raw.length() >= 8 && raw.chars().limit(8).allMatch(Character::isDigit)) {
                String ymd = raw.substring(0, 8);
                return LocalDateTime.parse(ymd + "000000", COMPACT_DT);
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static TransactionType parseTransactionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return TransactionType.INSERT_ORDER;
        }
        String s = raw.toLowerCase(Locale.ROOT);
        if (s.contains("finalize")) {
            return TransactionType.FINALIZE_ORDER;
        }
        if (s.contains("update")) {
            return TransactionType.UPDATE_ORDER;
        }
        return TransactionType.INSERT_ORDER;
    }

    private static LocalDateTime parseCompactDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.length() >= 14) {
            String head = s.substring(0, 14);
            if (head.chars().allMatch(Character::isDigit)) {
                try {
                    return LocalDateTime.parse(head, COMPACT_DT);
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return null;
    }

    /** ISO-8601, compact {@link #COMPACT_DT}, or date-only → start of day. */
    private static LocalDateTime parseDateTimeFlexible(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            return OffsetDateTime.parse(s).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        LocalDateTime compact = parseCompactDateTime(s);
        if (compact != null) {
            return compact;
        }
        try {
            if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
                return LocalDate.parse(s.substring(0, 10)).atStartOfDay();
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static Boolean parseBooleanFlexible(String raw) {
        Boolean b = parseBoolean01(raw);
        if (b != null) {
            return b;
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.trim();
        if (t.equalsIgnoreCase("Y") || t.equalsIgnoreCase("YES")) {
            return true;
        }
        if (t.equalsIgnoreCase("N") || t.equalsIgnoreCase("NO")) {
            return false;
        }
        return null;
    }

    private static BigDecimal parseBigDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean01(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if ("1".equals(s) || "true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s)) {
            return true;
        }
        if ("0".equals(s) || "false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s)) {
            return false;
        }
        return null;
    }

    private static String firstOf(Map<String, String> f, String... keys) {
        for (String k : keys) {
            String v = f.get(k);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String stringify(Object v) {
        if (v == null) {
            return "";
        }
        return Objects.toString(v, "").trim();
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
