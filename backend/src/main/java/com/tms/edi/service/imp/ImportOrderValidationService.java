package com.tms.edi.service.imp;

import com.tms.edi.entity.cfg.CommunicationPartner;
import com.tms.edi.entity.imp.*;
import com.tms.edi.entity.tms.TmsAddress;
import com.tms.edi.enums.MappingType;
import com.tms.edi.repository.imp.*;
import com.tms.edi.repository.tms.TmsAddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates EDI Import Orders before processing.
 *
 * Java port of AL codeunit 71102979 "Go4IMP Order-Check".
 *
 * Key design notes:
 *  - Each check method returns a list of error messages (empty = no errors).
 *  - AL used a single Text[250] error accumulator; we use a List<String> for richer feedback.
 *  - AL "CheckMapping returns TRUE on error" → our checkMapping() also returns true on missing mapping.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportOrderValidationService {

    private final ImportMappingService mappingService;
    private final ImportOrderLineRepository lineRepo;
    private final ImportOrderCargoRepository cargoRepo;
    private final ImportOrderReferenceRepository refRepo;
    private final ImportOrderEquipmentRepository equipRepo;
    private final ImportTransportCostRepository costRepo;
    private final TmsAddressRepository addressRepo;

    // ─────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT  (AL: PerformChecks)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Runs all checks for a given import order header.
     *
     * @return list of validation errors; empty list = order is valid
     */
    public List<String> performChecks(ImportOrderHeader header) {
        List<String> errors = new ArrayList<>();

        // 1. Mandatory field checks on header
        errors.addAll(testFieldsOrder(header));
        if (!errors.isEmpty()) return errors;

        // 2. Mapping checks on header
        errors.addAll(orderMapCheck(header));
        if (!errors.isEmpty()) return errors;

        // 3. Lines
        List<ImportOrderLine> lines = lineRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());
        for (ImportOrderLine line : lines) {
            errors.addAll(testFieldsLine(line));
            errors.addAll(lineMapCheck(line, header));
            if (!errors.isEmpty()) return errors;
        }

        // 4. Cargo
        List<ImportOrderCargo> cargoList = cargoRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());
        for (ImportOrderCargo cargo : cargoList) {
            errors.addAll(testFieldsCargo(cargo, header));
            errors.addAll(cargoMapCheck(cargo));
            if (!errors.isEmpty()) return errors;
        }

        // 5. References
        List<ImportOrderReference> refs = refRepo.findByIdEntryNoAndOriginalFalse(header.getEntryNo());
        for (ImportOrderReference ref : refs) {
            errors.addAll(testFieldsRef(ref));
            errors.addAll(refMapCheck(ref));
            if (!errors.isEmpty()) return errors;

            errors.addAll(checkDuplicateReference(ref,
                    mappingService.getMapping(header.getCommunicationPartner(),
                            MappingType.CUSTOMER, header.getExternalCustomerNo())));
            if (!errors.isEmpty()) return errors;
        }

        // 6. Equipment
        List<ImportOrderEquipment> equipments = equipRepo.findByIdEntryNo(header.getEntryNo());
        for (ImportOrderEquipment eq : equipments) {
            errors.addAll(equipMapCheck(eq));
            if (!errors.isEmpty()) return errors;
        }

        // 7. Transport costs / revenue
        List<ImportTransportCost> costs = costRepo.findByIdEntryNo(header.getEntryNo());
        for (ImportTransportCost cost : costs) {
            errors.addAll(revenueMapCheck(cost));
            if (!errors.isEmpty()) return errors;
        }

        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // ORDER HEADER CHECKS  (AL: TestFieldsOrder)
    // ─────────────────────────────────────────────────────────────────

    public List<String> testFieldsOrder(ImportOrderHeader h) {
        List<String> errors = new ArrayList<>();

        if (h.getExternalOrderNo() == null || h.getExternalOrderNo().isBlank()) {
            errors.add("External Order No. in Import Order Header is empty");
            return errors;
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // ORDER LINE CHECKS  (AL: TestFieldsLine)
    // ─────────────────────────────────────────────────────────────────

    public List<String> testFieldsLine(ImportOrderLine line) {
        List<String> errors = new ArrayList<>();

        if (line.getExternalOrderNo() == null || line.getExternalOrderNo().isBlank()) {
            errors.add("External Order No. in Import Order Line (line " + line.getId().getLineNo() + ") is empty");
            return errors;
        }
        if (line.getId().getLineNo() == null || line.getId().getLineNo() == 0) {
            errors.add("Line No. in Import Order Line is zero");
            return errors;
        }
        if (line.getActionCode() == null || line.getActionCode().isBlank()) {
            errors.add("Action Code in Import Order Line (line " + line.getId().getLineNo() + ") is empty");
            return errors;
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // CARGO CHECKS  (AL: TestFieldsCargo)
    // ─────────────────────────────────────────────────────────────────

    public List<String> testFieldsCargo(ImportOrderCargo cargo, ImportOrderHeader header) {
        List<String> errors = new ArrayList<>();

        if (cargo.getExternalOrderNo() == null || cargo.getExternalOrderNo().isBlank()) {
            errors.add("External Order No. in Import Order Cargo (line " + cargo.getId().getLineNo() + ") is empty");
            return errors;
        }
        if (cargo.getId().getLineNo() == null || cargo.getId().getLineNo() == 0) {
            errors.add("Line No. in Import Order Cargo is zero");
            return errors;
        }
        if (cargo.getExternalGoodNo() == null || cargo.getExternalGoodNo().isBlank()) {
            errors.add("External Good No. in Cargo (line " + cargo.getId().getLineNo() + ") is empty");
            return errors;
        }
        if (cargo.getQuantity() == null || cargo.getQuantity().signum() == 0) {
            errors.add("Quantity in Import Order Cargo (line " + cargo.getId().getLineNo() + ") is zero");
            return errors;
        }

        // ADR check: if ADR fields are set but ADR Type is blank
        if ((cargo.getAdrType() == null || cargo.getAdrType().isBlank()) &&
                hasAdrData(cargo)) {

            CommunicationPartner partner = mappingService.getPartner(cargo.getCommunicationPartner());
            if (!Boolean.TRUE.equals(partner.getAdrMapping())) {
                errors.add("Please activate the ADR Mapping feature for the Import Order Processing.");
                return errors;
            }
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // REFERENCE CHECKS  (AL: TestFieldsRef)
    // ─────────────────────────────────────────────────────────────────

    public List<String> testFieldsRef(ImportOrderReference ref) {
        List<String> errors = new ArrayList<>();

        if (ref.getExternalOrderNo() == null || ref.getExternalOrderNo().isBlank()) {
            errors.add("External Order No. in Import Order Reference (line " + ref.getId().getLineNo() + ") is empty");
            return errors;
        }
        if (ref.getId().getLineNo() == null || ref.getId().getLineNo() == 0) {
            errors.add("Line No. in Import Order Reference is zero");
            return errors;
        }
        if (ref.getReferenceCode() == null || ref.getReferenceCode().isBlank()) {
            errors.add("Reference Code in Import Order Reference (line " + ref.getId().getLineNo() + ") is empty");
            return errors;
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // ORDER HEADER MAPPING CHECKS  (AL: OrderMapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> orderMapCheck(ImportOrderHeader h) {
        List<String> errors = new ArrayList<>();
        String partner = h.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (!Boolean.TRUE.equals(cp.getCustomerNoIsInternal()) &&
                hasValue(h.getExternalCustomerNo()) &&
                mappingService.checkMapping(partner, MappingType.CUSTOMER, h.getExternalCustomerNo())) {
            errors.add("External Customer No. '" + h.getExternalCustomerNo() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getTransportTypeIsInternal()) &&
                hasValue(h.getTransportType()) &&
                mappingService.checkMapping(partner, MappingType.TRANSPORT_TYPE, normalize(h.getTransportType()))) {
            List<String> mappedTransportTypes = mappingService.getMappedForeignIds(partner, MappingType.TRANSPORT_TYPE);
            log.warn("Transport Type mapping failed: partner={}, incoming='{}', availableMappings={}",
                    partner, normalize(h.getTransportType()), mappedTransportTypes);
            errors.add("Transport Type is not configured. Please map it in master data. Received: '" + normalize(h.getTransportType()) + "'");
        }

        if (!Boolean.TRUE.equals(cp.getOfficeIsInternal()) &&
                hasValue(h.getOffice()) &&
                mappingService.checkMapping(partner, MappingType.OFFICE, h.getOffice())) {
            errors.add("Office '" + h.getOffice() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getCustServRespIsInternal()) &&
                hasValue(h.getCustServResponsible()) &&
                mappingService.checkMapping(partner, MappingType.CS_RESPONSIBLE, h.getCustServResponsible())) {
            errors.add("Customer Service Responsible '" + h.getCustServResponsible() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getSalesResponsibleIsInternal()) &&
                hasValue(h.getSalesResponsible()) &&
                mappingService.checkMapping(partner, MappingType.SALES_RESPONSIBLE, h.getSalesResponsible())) {
            errors.add("Sales Responsible '" + h.getSalesResponsible() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getTripTypeIsInternal()) &&
                hasValue(h.getTripTypeNo()) &&
                mappingService.checkMapping(partner, MappingType.TRIP_TYPE, h.getTripTypeNo())) {
            errors.add("Trip Type No. '" + h.getTripTypeNo() + "' is not mapped properly.");
        }

        if (hasValue(h.getCarrierNo()) &&
                !Boolean.TRUE.equals(cp.getCarrierIsInternal()) &&
                mappingService.checkMapping(partner, MappingType.CARRIER, h.getCarrierNo())) {
            errors.add("Carrier No. '" + h.getCarrierNo() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getCountryCodeIsInternal()) &&
                hasValue(h.getCountryOfOrigin()) &&
                mappingService.checkMapping(partner, MappingType.COUNTRY, h.getCountryOfOrigin())) {
            errors.add("Country of Origin '" + h.getCountryOfOrigin() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getCountryCodeIsInternal()) &&
                hasValue(h.getCountryOfDestination()) &&
                mappingService.checkMapping(partner, MappingType.COUNTRY, h.getCountryOfDestination())) {
            errors.add("Country of Destination '" + h.getCountryOfDestination() + "' is not mapped properly.");
        }

        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // ORDER LINE MAPPING CHECKS  (AL: LineMapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> lineMapCheck(ImportOrderLine line, ImportOrderHeader header) {
        List<String> errors = new ArrayList<>();
        String partner = line.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        // Action Code mapping
        if (!Boolean.TRUE.equals(cp.getActionCodeIsInternal()) &&
                hasValue(line.getActionCode()) &&
                mappingService.checkMapping(partner, MappingType.ACTION, line.getActionCode())) {
            errors.add("Action Code '" + line.getActionCode() + "' (line " + line.getId().getLineNo() + ") is not mapped properly.");
        }

        // Address mapping
        if (!Boolean.TRUE.equals(cp.getAddressNoIsInternal()) &&
                hasValue(line.getExternalAddressNo())) {

            if ("0".equals(line.getExternalAddressNo())) {
                errors.add("Cannot map or insert address with 'External Address No.' = '0' (line " + line.getId().getLineNo() + ")");
                return errors;
            }

            if (mappingService.checkMapping(partner, MappingType.ADDRESS, line.getExternalAddressNo())) {
                // Auto-insert address if configured
                if (Boolean.TRUE.equals(cp.getAutoInsertAddress())) {
                    if (!hasValue(line.getAddressCountryCode())) {
                        errors.add("External Address No. '" + line.getExternalAddressNo() + "' - address country code is empty");
                    } else if (!Boolean.TRUE.equals(cp.getCountryCodeIsInternal())) {
                        String localCountry = mappingService.getMapping(partner, MappingType.COUNTRY, line.getAddressCountryCode());
                        if (localCountry.isBlank()) {
                            errors.add("Country Code '" + line.getAddressCountryCode() + "' is not mapped properly.");
                        }
                        // City and address auto-insertion deferred to processing service
                    }
                } else {
                    errors.add("External Address No. '" + line.getExternalAddressNo() + "' (line " + line.getId().getLineNo() + ") is not mapped properly.");
                }
            }
        }

        // Check that mapped address has required fields (coordinates if xServer active)
        String mappedAddressNo = mappingService.getMapping(partner, MappingType.ADDRESS, line.getExternalAddressNo());
        if (!mappedAddressNo.isBlank()) {
            Optional<TmsAddress> addrOpt = addressRepo.findById(mappedAddressNo);
            if (addrOpt.isEmpty()) {
                errors.add("External Address No. '" + line.getExternalAddressNo() + "' is not mapped to a valid TMS Address.");
            } else {
                TmsAddress addr = addrOpt.get();
                if (!hasValue(addr.getCountryCode()) || !hasValue(addr.getCity()) ||
                        !hasValue(addr.getPostalCode())) {
                    errors.add("Invalid mapped TMS Address - " + addr.getNo() + " " + addr.getName() +
                            " (missing country/city/postal code)");
                }
                // Address re-validation
                if (Boolean.TRUE.equals(cp.getRevalidateAddressMapping())) {
                    errors.addAll(checkAdrMapValidity(addr, line));
                }
            }
        }

        // DateTime: Until must not be before From
        if (line.getInitialDatetimeFrom() != null && line.getInitialDatetimeUntil() != null &&
                line.getInitialDatetimeUntil().isBefore(line.getInitialDatetimeFrom())) {
            errors.add("Initial DateTime Until is before Initial DateTime From on line " + line.getId().getLineNo());
        }

        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // CARGO MAPPING CHECKS  (AL: CargoMapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> cargoMapCheck(ImportOrderCargo cargo) {
        List<String> errors = new ArrayList<>();
        String partner = cargo.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (!Boolean.TRUE.equals(cp.getGoodNoIsInternal()) &&
                hasValue(cargo.getExternalGoodNo()) &&
                mappingService.checkMapping(partner, MappingType.GOOD_NUMBER, cargo.getExternalGoodNo())) {
            errors.add("External Good No. '" + cargo.getExternalGoodNo() + "' (cargo line " + cargo.getId().getLineNo() + ") is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getUomCodeIsInternal()) &&
                hasValue(cargo.getUnitOfMeasureCode()) &&
                mappingService.checkMapping(partner, MappingType.UNIT_OF_MEASURE, cargo.getUnitOfMeasureCode())) {
            errors.add("Unit of Measure '" + cargo.getUnitOfMeasureCode() + "' (cargo line " + cargo.getId().getLineNo() + ") is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getGoodTypeIsInternal()) &&
                hasValue(cargo.getExternalGoodType()) &&
                mappingService.checkMapping(partner, MappingType.GOOD_TYPE, cargo.getExternalGoodType())) {
            errors.add("External Good Type '" + cargo.getExternalGoodType() + "' (cargo line " + cargo.getId().getLineNo() + ") is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getGoodSubTypeIsInternal()) &&
                hasValue(cargo.getExternalGoodSubType()) &&
                mappingService.checkMapping(partner, MappingType.GOOD_SUB_TYPE, cargo.getExternalGoodSubType())) {
            errors.add("External Good Sub Type '" + cargo.getExternalGoodSubType() + "' (cargo line " + cargo.getId().getLineNo() + ") is not mapped properly.");
        }

        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // REFERENCE MAPPING CHECKS  (AL: RefmapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> refMapCheck(ImportOrderReference ref) {
        List<String> errors = new ArrayList<>();
        String partner = ref.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (!Boolean.TRUE.equals(cp.getReferenceCodeIsInternal()) &&
                hasValue(ref.getReferenceCode()) &&
                mappingService.checkMapping(partner, MappingType.REFERENCE_CODE, ref.getReferenceCode())) {
            errors.add("Reference Code '" + ref.getReferenceCode() + "' (ref line " + ref.getId().getLineNo() + ") is not mapped properly.");
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // EQUIPMENT MAPPING CHECKS  (AL: EquipmapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> equipMapCheck(ImportOrderEquipment eq) {
        List<String> errors = new ArrayList<>();
        String partner = eq.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (!Boolean.TRUE.equals(cp.getEquipmentTypeIsInternal()) &&
                hasValue(eq.getEquipmentTypeNo()) &&
                mappingService.checkMapping(partner, MappingType.EQUIPMENT_TYPE, eq.getEquipmentTypeNo())) {
            errors.add("Equipment Type No. '" + eq.getEquipmentTypeNo() + "' (eq line " + eq.getId().getLineNo() + ") is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getEquipmentSubTypeIsInternal()) &&
                hasValue(eq.getEquipmentSubTypeNo()) &&
                mappingService.checkMapping(partner, MappingType.EQ_SUB_TYPE, eq.getEquipmentSubTypeNo())) {
            errors.add("Equipment Sub Type No. '" + eq.getEquipmentSubTypeNo() + "' (eq line " + eq.getId().getLineNo() + ") is not mapped properly.");
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // REVENUE / COST MAPPING CHECKS  (AL: RevenueMapCheck)
    // ─────────────────────────────────────────────────────────────────

    public List<String> revenueMapCheck(ImportTransportCost cost) {
        List<String> errors = new ArrayList<>();
        String partner = cost.getCommunicationPartner();
        CommunicationPartner cp = mappingService.getPartner(partner);

        if (!Boolean.TRUE.equals(cp.getRevenueIsInternal()) &&
                hasValue(cost.getRevenueCode()) &&
                mappingService.checkMapping(partner, MappingType.REVENUE, cost.getRevenueCode())) {
            errors.add("Revenue Code '" + cost.getRevenueCode() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getRevenueTypeIsInternal()) &&
                hasValue(cost.getRevenueType()) &&
                mappingService.checkMapping(partner, MappingType.REVENUE_TYPE, cost.getRevenueType())) {
            errors.add("Revenue Type '" + cost.getRevenueType() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getUomCodeIsInternal()) &&
                hasValue(cost.getUnitOfMeasureBudget()) &&
                mappingService.checkMapping(partner, MappingType.UNIT_OF_MEASURE, cost.getUnitOfMeasureBudget())) {
            errors.add("Unit of Measure Budget '" + cost.getUnitOfMeasureBudget() + "' is not mapped properly.");
        }

        if (!Boolean.TRUE.equals(cp.getCurrencyCodeIsInternal()) &&
                hasValue(cost.getCurrencyActual()) &&
                mappingService.checkMapping(partner, MappingType.CURRENCY_CODE, cost.getCurrencyActual())) {
            errors.add("Currency Actual '" + cost.getCurrencyActual() + "' is not mapped properly.");
        }

        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // DUPLICATE REFERENCE CHECK  (AL: CheckDuplicateReferenceEv)
    // ─────────────────────────────────────────────────────────────────

    public List<String> checkDuplicateReference(ImportOrderReference ref, String customerNo) {
        // Simplified: in a real system this would also query TmsOrderReference table
        // for existing orders with the same reference value
        return new ArrayList<>();
    }

    // ─────────────────────────────────────────────────────────────────
    // ADDRESS RE-VALIDATION  (AL: CheckAdrMapValidity)
    // ─────────────────────────────────────────────────────────────────

    private List<String> checkAdrMapValidity(TmsAddress tmsAddr, ImportOrderLine line) {
        List<String> errors = new ArrayList<>();

        if (hasValue(line.getAddressName()) &&
                !normalize(line.getAddressName()).equalsIgnoreCase(normalize(tmsAddr.getName()))) {
            errors.add("Imported Address '" + line.getAddressName() + "' (" + line.getExternalAddressNo() +
                    ") does not match with the mapped TMS Address: " + tmsAddr.getNo());
            return errors;
        }
        if (hasValue(line.getAddressCity()) &&
                !normalize(line.getAddressCity()).equalsIgnoreCase(normalize(tmsAddr.getCity()))) {
            errors.add("Imported Address city '" + line.getAddressCity() + "' does not match TMS Address " + tmsAddr.getNo());
        }
        return errors;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────

    private boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private boolean hasAdrData(ImportOrderCargo cargo) {
        return hasValue(cargo.getAdrUnNo()) ||
                hasValue(cargo.getAdrTunnelRestrictionCode()) ||
                hasValue(cargo.getAdrPackingGroup()) ||
                hasValue(cargo.getAdrHazardClass());
    }

    /** Trims leading/trailing spaces — mirrors AL DelChr(value, '<>') */
    private String normalize(String s) {
        return s == null ? "" : s.strip();
    }
}
