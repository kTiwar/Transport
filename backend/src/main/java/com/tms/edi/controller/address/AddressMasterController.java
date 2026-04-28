package com.tms.edi.controller.address;

import com.tms.edi.dto.address.*;
import com.tms.edi.entity.address.*;
import com.tms.edi.repository.address.*;
import com.tms.edi.service.address.AddressMasterImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/address-master")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AddressMasterController {

    private final AddressMasterRepository addressMasterRepository;
    private final AddressContactRepository contactRepository;
    private final AddressAttributeRepository attributeRepository;
    private final AddressUsageRepository usageRepository;
    private final AddressI18nRepository i18nRepository;
    private final AddressAuditRepository auditRepository;
    private final AddressMasterImportService addressMasterImportService;

    @GetMapping
    public ResponseEntity<Page<AddressMasterSummaryDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q) {
        String trimmed = StringUtils.hasText(q) ? q.trim() : null;
        Page<AddressMaster> raw = StringUtils.hasText(trimmed)
                ? addressMasterRepository.search(trimmed, PageRequest.of(page, size, Sort.by("addressCode").ascending()))
                : addressMasterRepository.findAll(PageRequest.of(page, size, Sort.by("addressCode").ascending()));
        return ResponseEntity.ok(raw.map(this::toSummary));
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressMasterDetailDto> getOne(@PathVariable Long addressId) {
        AddressMaster m = addressMasterRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        AddressMasterDetailDto dto = toDetail(m);
        dto.setContacts(contactRepository.findByAddressIdOrderByContactIdAsc(addressId).stream()
                .map(this::toContactDto).collect(Collectors.toList()));
        dto.setAttributes(attributeRepository.findByAddressIdOrderByAttrIdAsc(addressId).stream()
                .map(this::toAttributeDto).collect(Collectors.toList()));
        dto.setUsages(usageRepository.findByAddressIdOrderByPriorityAscUsageIdAsc(addressId).stream()
                .map(this::toUsageDto).collect(Collectors.toList()));
        dto.setTranslations(i18nRepository.findByAddressIdOrderByLanguageCodeAsc(addressId).stream()
                .map(this::toI18nDto).collect(Collectors.toList()));
        dto.setAuditTrail(auditRepository.findByAddressIdOrderByChangedAtDesc(addressId).stream()
                .map(this::toAuditDto).collect(Collectors.toList()));

        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody AddressMasterUpsertDto body) {
        if (body == null || !StringUtils.hasText(body.getAddressCode())) {
            return badRequest("Address code is required.", "addressCode");
        }
        if (!StringUtils.hasText(body.getAddressType())) {
            return badRequest("Address type is required.", "addressType");
        }
        String code = body.getAddressCode().trim();
        if (addressMasterRepository.findByAddressCode(code).isPresent()) {
            return badRequest("Address code already exists.", "addressCode");
        }
        AddressMaster saved = addressMasterRepository.save(applyUpsert(AddressMaster.builder().addressCode(code).build(), body));
        saveAudit(saved.getAddressId(), "SYSTEM", null, snapshot(saved));
        return ResponseEntity.ok(toDetail(saved));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<?> update(@PathVariable Long addressId, @RequestBody AddressMasterUpsertDto body) {
        if (body == null || !StringUtils.hasText(body.getAddressCode())) {
            return badRequest("Address code is required.", "addressCode");
        }
        AddressMaster existing = addressMasterRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));

        String code = body.getAddressCode().trim();
        if (addressMasterRepository.findByAddressCode(code).filter(it -> !it.getAddressId().equals(addressId)).isPresent()) {
            return badRequest("Address code already exists.", "addressCode");
        }

        String oldSnap = snapshot(existing);
        existing.setAddressCode(code);
        AddressMaster saved = addressMasterRepository.save(applyUpsert(existing, body));
        saveAudit(saved.getAddressId(), "SYSTEM", oldSnap, snapshot(saved));
        return ResponseEntity.ok(toDetail(saved));
    }

    @PatchMapping("/{addressId}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Long addressId) {
        AddressMaster existing = addressMasterRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        String oldSnap = snapshot(existing);
        existing.setIsActive(false);
        AddressMaster saved = addressMasterRepository.save(existing);
        saveAudit(saved.getAddressId(), "SYSTEM", oldSnap, snapshot(saved));
        return ResponseEntity.ok(toSummary(saved));
    }

    @PatchMapping("/{addressId}/activate")
    public ResponseEntity<?> activate(@PathVariable Long addressId) {
        AddressMaster existing = addressMasterRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        String oldSnap = snapshot(existing);
        existing.setIsActive(true);
        AddressMaster saved = addressMasterRepository.save(existing);
        saveAudit(saved.getAddressId(), "SYSTEM", oldSnap, snapshot(saved));
        return ResponseEntity.ok(toSummary(saved));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<?> delete(@PathVariable Long addressId) {
        AddressMaster existing = addressMasterRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        saveAudit(existing.getAddressId(), "SYSTEM", snapshot(existing), "DELETED");
        addressMasterRepository.deleteById(addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AddressMasterImportResultDto> importExcel(@RequestPart("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    AddressMasterImportResultDto.builder().errors(List.of("No file uploaded.")).build());
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body(
                    AddressMasterImportResultDto.builder().errors(List.of("Only .xlsx files are supported.")).build());
        }
        return ResponseEntity.ok(addressMasterImportService.importExcel(file));
    }

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadImportTemplate() throws Exception {
        byte[] bytes = addressMasterImportService.buildImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"address-master-import-template.xlsx\"")
                .body(bytes);
    }

    private ResponseEntity<Map<String, String>> badRequest(String message, String field) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("message", message);
        out.put("field", field);
        return ResponseEntity.badRequest().body(out);
    }

    private String snapshot(AddressMaster m) {
        return "code=" + m.getAddressCode() + "; type=" + m.getAddressType() + "; city=" + m.getCity() + "; postal=" + m.getPostalCode() + "; active=" + m.getIsActive();
    }

    private void saveAudit(Long addressId, String changedBy, String oldValue, String newValue) {
        auditRepository.save(AddressAudit.builder()
                .addressId(addressId)
                .changedBy(changedBy)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(LocalDateTime.now())
                .build());
    }

    private AddressMaster applyUpsert(AddressMaster m, AddressMasterUpsertDto body) {
        m.setAddressType(body.getAddressType());
        m.setEntityType(body.getEntityType());
        m.setEntityId(body.getEntityId());
        m.setAddressLine1(body.getAddressLine1());
        m.setAddressLine2(body.getAddressLine2());
        m.setAddressLine3(body.getAddressLine3());
        m.setLandmark(body.getLandmark());
        m.setCity(body.getCity());
        m.setDistrict(body.getDistrict());
        m.setStateProvince(body.getStateProvince());
        m.setPostalCode(body.getPostalCode());
        m.setCountryCode(body.getCountryCode());
        m.setCountryName(body.getCountryName());
        m.setLatitude(body.getLatitude());
        m.setLongitude(body.getLongitude());
        m.setTimezone(body.getTimezone());
        m.setIsPrimary(body.getIsPrimary() != null ? body.getIsPrimary() : false);
        m.setIsActive(body.getIsActive() != null ? body.getIsActive() : true);
        m.setValidationStatus(body.getValidationStatus());
        return m;
    }

    private AddressMasterSummaryDto toSummary(AddressMaster m) {
        return AddressMasterSummaryDto.builder()
                .addressId(m.getAddressId())
                .addressCode(m.getAddressCode())
                .addressType(m.getAddressType())
                .entityType(m.getEntityType())
                .entityId(m.getEntityId())
                .city(m.getCity())
                .postalCode(m.getPostalCode())
                .countryCode(m.getCountryCode())
                .isPrimary(m.getIsPrimary())
                .isActive(m.getIsActive())
                .validationStatus(m.getValidationStatus())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private AddressMasterDetailDto toDetail(AddressMaster m) {
        return AddressMasterDetailDto.builder()
                .addressId(m.getAddressId())
                .addressCode(m.getAddressCode())
                .addressType(m.getAddressType())
                .entityType(m.getEntityType())
                .entityId(m.getEntityId())
                .addressLine1(m.getAddressLine1())
                .addressLine2(m.getAddressLine2())
                .addressLine3(m.getAddressLine3())
                .landmark(m.getLandmark())
                .city(m.getCity())
                .district(m.getDistrict())
                .stateProvince(m.getStateProvince())
                .postalCode(m.getPostalCode())
                .countryCode(m.getCountryCode())
                .countryName(m.getCountryName())
                .latitude(m.getLatitude())
                .longitude(m.getLongitude())
                .timezone(m.getTimezone())
                .isPrimary(m.getIsPrimary())
                .isActive(m.getIsActive())
                .validationStatus(m.getValidationStatus())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private AddressContactDto toContactDto(AddressContact c) {
        return AddressContactDto.builder()
                .contactId(c.getContactId())
                .contactName(c.getContactName())
                .phoneNumber(c.getPhoneNumber())
                .alternatePhone(c.getAlternatePhone())
                .email(c.getEmail())
                .isPrimaryContact(c.getIsPrimaryContact())
                .build();
    }

    private AddressAttributeDto toAttributeDto(AddressAttribute a) {
        return AddressAttributeDto.builder()
                .attrId(a.getAttrId())
                .attrKey(a.getAttrKey())
                .attrValue(a.getAttrValue())
                .build();
    }

    private AddressUsageDto toUsageDto(AddressUsage u) {
        return AddressUsageDto.builder()
                .usageId(u.getUsageId())
                .usageType(u.getUsageType())
                .priority(u.getPriority())
                .build();
    }

    private AddressI18nDto toI18nDto(AddressI18n i) {
        return AddressI18nDto.builder()
                .id(i.getId())
                .languageCode(i.getLanguageCode())
                .addressText(i.getAddressText())
                .build();
    }

    private AddressAuditDto toAuditDto(AddressAudit a) {
        return AddressAuditDto.builder()
                .auditId(a.getAuditId())
                .changedBy(a.getChangedBy())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .changedAt(a.getChangedAt())
                .build();
    }
}