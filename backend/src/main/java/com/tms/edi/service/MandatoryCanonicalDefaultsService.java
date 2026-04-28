package com.tms.edi.service;

import com.tms.edi.canonical.CanonicalOrder;
import com.tms.edi.entity.TmsFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Fills mandatory canonical header fields when the source file has no value and mapping did not populate them.
 */
@Slf4j
@Service
public class MandatoryCanonicalDefaultsService {

    private static final int MAX_EXTERNAL_ORDER_ID = 100;
    private static final int MAX_CUSTOMER_CODE = 50;

    /**
     * Mutates {@code order} when {@code external_order_id}, {@code customer_code}, or {@code order_date} are missing.
     */
    public void applyHeaderDefaultsIfMissing(CanonicalOrder order, TmsFile tmsFile, int recordIndex) {
        if (order == null || tmsFile == null) {
            return;
        }
        List<String> applied = new ArrayList<>();

        if (isBlank(order.getExternalOrderId())) {
            Long entryNo = tmsFile.getEntryNo() != null ? tmsFile.getEntryNo() : 0L;
            String gen = buildGeneratedExternalOrderId(entryNo, recordIndex);
            order.setExternalOrderId(gen);
            applied.add("external_order_id");
        }
        if (isBlank(order.getCustomerCode())) {
            String partnerCode = tmsFile.getPartner() != null ? tmsFile.getPartner().getPartnerCode() : null;
            String pc = sanitizeCode(partnerCode, 40);
            String cust = "DEFAULT-" + pc;
            if (cust.length() > MAX_CUSTOMER_CODE) {
                cust = cust.substring(0, MAX_CUSTOMER_CODE);
            }
            order.setCustomerCode(cust);
            applied.add("customer_code");
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
            applied.add("order_date");
        }

        if (!applied.isEmpty()) {
            String msg = "[System defaults applied: " + String.join(", ", applied) + "]";
            appendNote(order, msg);
            log.info("File entry {} record {} — {}", tmsFile.getEntryNo(), recordIndex, msg);
        }
    }

    private static String buildGeneratedExternalOrderId(Long entryNo, int recordIndex) {
        String base = "AUTO-" + entryNo + "-R" + recordIndex + "-" +
                UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return base.length() <= MAX_EXTERNAL_ORDER_ID ? base : base.substring(0, MAX_EXTERNAL_ORDER_ID);
    }

    private static String sanitizeCode(String raw, int maxLen) {
        if (raw == null || raw.isBlank()) {
            return "UNK";
        }
        String s = raw.replaceAll("[^a-zA-Z0-9_-]", "").toUpperCase();
        if (s.isEmpty()) {
            return "UNK";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private static void appendNote(CanonicalOrder order, String line) {
        String n = order.getNotes();
        if (n == null || n.isBlank()) {
            order.setNotes(line);
        } else {
            order.setNotes(n + "\n" + line);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
