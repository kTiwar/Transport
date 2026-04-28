package com.tms.edi.service;

import com.tms.edi.canonical.CanonicalOrder;
import com.tms.edi.dto.imp.ImportOrderHeaderDto;
import com.tms.edi.entity.TmsFile;
import com.tms.edi.repository.cfg.CommunicationPartnerRepository;
import com.tms.edi.service.imp.ImportOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * After EDI file staging succeeds, creates a Go4IMP import order and runs TMS processing
 * in a separate transaction so import/TMS failures do not roll back {@code edi_order_*} rows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EdiImportOrderChainingService {

    private static final int MAX_EXT_ORDER = 80;
    private static final int MAX_EXT_CUST = 30;
    private static final int MAX_COMM_PARTNER = 20;

    private final CommunicationPartnerRepository communicationPartnerRepository;
    private final ImportOrderService importOrderService;

    @Value("${tms.edi.auto-chain-import-order:true}")
    private boolean autoChainImportOrder;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void pushFromEdi(CanonicalOrder canonical, TmsFile tmsFile, Map<String, Object> flatRecord) {
        if (!autoChainImportOrder) {
            return;
        }
        if (canonical == null || tmsFile == null) {
            return;
        }
        try {
            String extOrder = truncate(canonical.getExternalOrderId(), MAX_EXT_ORDER);
            if (extOrder == null || extOrder.isBlank()) {
                log.warn("Skip EDI→Import chain for file {}: no external order id", tmsFile.getEntryNo());
                return;
            }

            String comm = resolveCommunicationPartner(tmsFile);
            Map<String, Object> rec = flatRecord != null ? flatRecord : Map.of();
            ImportOrderHeaderDto dto = EdiFlatRecordToImportOrderMapper.build(
                    canonical,
                    comm,
                    extOrder,
                    truncate(canonical.getCustomerCode(), MAX_EXT_CUST),
                    rec);
            dto.setImportFileEntryNo(tmsFile.getEntryNo());

            var saved = importOrderService.receiveOrder(dto);
            var proc = importOrderService.processOrder(saved.getEntryNo());
            String tmsNo = proc.primaryTmsOrderNo();
            if (tmsNo == null || tmsNo.isBlank()) {
                log.warn("EDI file entry {} → import entry {} did not yield a TMS order (check Import Orders)",
                        tmsFile.getEntryNo(), saved.getEntryNo());
            } else {
                log.info("EDI file entry {} → import entry {} → TMS order {}",
                        tmsFile.getEntryNo(), saved.getEntryNo(), tmsNo);
            }
        } catch (Exception ex) {
            log.error("EDI→Import→TMS chain failed for file entry {}: {}",
                    tmsFile.getEntryNo(), ex.getMessage(), ex);
        }
    }

    private String resolveCommunicationPartner(TmsFile tmsFile) {
        String raw = tmsFile.getPartner() != null ? tmsFile.getPartner().getPartnerCode() : null;
        if (raw == null || raw.isBlank()) {
            return "DEFAULT";
        }
        String code = raw.length() > MAX_COMM_PARTNER ? raw.substring(0, MAX_COMM_PARTNER) : raw;
        return communicationPartnerRepository.existsById(code) ? code : "DEFAULT";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
