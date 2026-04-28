package com.tms.edi.service.imp;

import com.tms.edi.entity.cfg.CommunicationPartner;
import com.tms.edi.entity.cfg.ImportMapping;
import com.tms.edi.enums.MappingType;
import com.tms.edi.repository.cfg.CommunicationPartnerRepository;
import com.tms.edi.repository.cfg.ImportMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Replicates AL "Go4IMP Import Mapping" functions:
 *  - GetMapping(partner, type, foreignId) → localId
 *  - CheckMapping(partner, type, foreignId) → true if NOT mapped (= error condition in AL)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportMappingService {

    private final ImportMappingRepository mappingRepo;
    private final CommunicationPartnerRepository partnerRepo;

    /**
     * Translates an external partner code to an internal TMS code.
     *
     * AL equivalent: Mapping.GetMapping(partner, type, foreignId)
     *
     * @return the localId, or empty string if no mapping found (same AL behaviour)
     */
    public String getMapping(String partner, MappingType type, String foreignId) {
        if (foreignId == null || foreignId.isBlank()) {
            return "";
        }
        String normalizedForeignId = foreignId.strip();
        return mappingRepo
                .findActiveByNormalizedForeignId(partner, type, normalizedForeignId)
                .map(ImportMapping::getLocalId)
                .orElse("");
    }

    /**
     * Checks whether a mapping is MISSING.
     *
     * AL equivalent: Mapping.CheckMapping(partner, type, foreignId)
     * In AL, this returns TRUE when the mapping does NOT exist (i.e. it is an error condition).
     *
     * @return true if mapping is MISSING (= error), false if mapping exists
     */
    public boolean checkMapping(String partner, MappingType type, String foreignId) {
        if (foreignId == null || foreignId.isBlank()) {
            return false;   // blank values are not checked
        }
        String localId = getMapping(partner, type, foreignId);
        boolean missing = localId.isBlank();
        log.info("Mapping check result: partner={}, type={}, incoming='{}', missing={}",
                partner, type, foreignId, missing);
        return missing;   // true = missing = error
    }

    public List<String> getMappedForeignIds(String partner, MappingType type) {
        return mappingRepo.findDistinctActiveForeignIds(partner, type);
    }

    /**
     * Returns the CommunicationPartner configuration, throws if not found.
     */
    public CommunicationPartner getPartner(String partnerCode) {
        return partnerRepo.findById(partnerCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Communication partner not found: " + partnerCode));
    }

    /**
     * Saves a new mapping entry (used by AutoInsertAddress logic).
     */
    @Transactional
    public ImportMapping saveMapping(String partner, MappingType type, String foreignId, String localId) {
        Optional<ImportMapping> existing = mappingRepo
                .findByCommunicationPartnerAndMappingTypeAndForeignIdAndActiveTrue(partner, type, foreignId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return mappingRepo.save(ImportMapping.builder()
                .communicationPartner(partner)
                .mappingType(type)
                .foreignId(foreignId)
                .localId(localId)
                .active(true)
                .build());
    }
}
