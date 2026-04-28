package com.tms.edi.service;

import com.tms.edi.canonical.CanonicalOrder;
import com.tms.edi.entity.EdiErrorLog;
import com.tms.edi.entity.TmsFile;
import com.tms.edi.enums.ErrorType;
import com.tms.edi.repository.EdiOrderHeaderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationService {

    private final EdiOrderHeaderRepository orderHeaderRepository;
    private final MandatoryCanonicalDefaultsService mandatoryDefaultsService;

    /**
     * Validates a canonical order and appends any validation errors to the list.
     * Returns true if the order can proceed to staging insertion.
     */
    public boolean validate(CanonicalOrder order, TmsFile tmsFile, List<EdiErrorLog> errors) {
        return validate(order, tmsFile, errors, 0);
    }

    /**
     * @param recordIndex row index within the file (used for generated external order ids)
     */
    public boolean validate(CanonicalOrder order, TmsFile tmsFile, List<EdiErrorLog> errors, int recordIndex) {
        mandatoryDefaultsService.applyHeaderDefaultsIfMissing(order, tmsFile, recordIndex);

        boolean valid = true;

        // Mandatory field checks
        if (isBlank(order.getExternalOrderId())) {
            errors.add(buildError(tmsFile, ErrorType.MISSING_MANDATORY_FIELD, "EDI-001",
                    "external_order_id is required but is null/empty", "external_order_id"));
            valid = false;
        }
        if (isBlank(order.getCustomerCode())) {
            errors.add(buildError(tmsFile, ErrorType.MISSING_MANDATORY_FIELD, "EDI-001",
                    "customer_code is required but is null/empty", "customer_code"));
            valid = false;
        }
        if (order.getOrderDate() == null) {
            errors.add(buildError(tmsFile, ErrorType.MISSING_MANDATORY_FIELD, "EDI-001",
                    "order_date is required but is null", "order_date"));
            valid = false;
        }

        // Duplicate order check
        if (!isBlank(order.getExternalOrderId())) {
            boolean exists = orderHeaderRepository.existsByExternalOrderIdAndPartner_PartnerId(
                    order.getExternalOrderId(), tmsFile.getPartner().getPartnerId());
            if (exists) {
                errors.add(buildError(tmsFile, ErrorType.DUPLICATE_ORDER_ID, "EDI-005",
                        "Duplicate order ID: " + order.getExternalOrderId() + " for partner "
                        + tmsFile.getPartner().getPartnerCode(), "external_order_id"));
                valid = false;
            }
        }

        // Line validation
        if (order.getLines() != null) {
            for (int i = 0; i < order.getLines().size(); i++) {
                var line = order.getLines().get(i);
                if (isBlank(line.getItemCode())) {
                    errors.add(buildError(tmsFile, ErrorType.MISSING_MANDATORY_FIELD, "EDI-001",
                            "lines[" + i + "].item_code is required", "lines[" + i + "].item_code"));
                    valid = false;
                }
                if (line.getQuantity() == null) {
                    errors.add(buildError(tmsFile, ErrorType.MISSING_MANDATORY_FIELD, "EDI-001",
                            "lines[" + i + "].quantity is required", "lines[" + i + "].quantity"));
                    valid = false;
                }
            }
        }

        return valid;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private EdiErrorLog buildError(TmsFile file, ErrorType type, String code,
                                    String message, String path) {
        return EdiErrorLog.builder()
                .tmsFile(file)
                .errorType(type)
                .errorCode(code)
                .errorMessage(message)
                .fieldPath(path)
                .build();
    }
}
