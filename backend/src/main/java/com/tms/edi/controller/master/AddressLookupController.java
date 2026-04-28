package com.tms.edi.controller.master;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tms.edi.dto.master.AddressLookupOptionDto;
import com.tms.edi.entity.master.ReferenceMaster;
import com.tms.edi.repository.master.ReferenceMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/address-lookups")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AddressLookupController {

    private static final List<String> CATEGORIES = List.of(
            "COUNTRY", "STATE", "CITY", "POSTAL_CODE", "ADDRESS_TYPE", "REGION", "ZONE"
    );

    private final ReferenceMasterRepository referenceMasterRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/categories")
    public List<String> categories() {
        return CATEGORIES;
    }

    @GetMapping("/options")
    public ResponseEntity<List<AddressLookupOptionDto>> options(
            @RequestParam String category,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String parentCode,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "100") int limit) {

        String cat = normalizedCategory(category);
        if (!CATEGORIES.contains(cat)) {
            return ResponseEntity.badRequest().build();
        }

        List<ReferenceMaster> source = activeOnly
                ? referenceMasterRepository.findByCategoryAndIsActiveOrderBySortOrderAscCodeAsc(cat, true)
                : referenceMasterRepository.findByCategoryOrderBySortOrderAscCodeAsc(
                cat, org.springframework.data.domain.PageRequest.of(0, Math.max(1, Math.min(limit * 2, 500))))
                .getContent();

        String parentKey = parentKey(cat);
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        String parent = parentCode == null ? "" : parentCode.trim().toUpperCase(Locale.ROOT);

        List<AddressLookupOptionDto> out = source.stream()
                .map(this::toDto)
                .filter(it -> {
                    if (!StringUtils.hasText(parent) || parentKey == null) return true;
                    String v = it.getExtra().get(parentKey);
                    return parent.equalsIgnoreCase(v);
                })
                .filter(it -> {
                    if (!StringUtils.hasText(query)) return true;
                    return it.getCode().toLowerCase(Locale.ROOT).contains(query)
                            || it.getName().toLowerCase(Locale.ROOT).contains(query)
                            || Optional.ofNullable(it.getDescription()).orElse("").toLowerCase(Locale.ROOT).contains(query);
                })
                .limit(Math.max(1, Math.min(limit, 200)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    @GetMapping("/by-code")
    public ResponseEntity<AddressLookupOptionDto> byCode(
            @RequestParam String category,
            @RequestParam String code) {
        String cat = normalizedCategory(category);
        if (!CATEGORIES.contains(cat) || !StringUtils.hasText(code)) {
            return ResponseEntity.badRequest().build();
        }
        return referenceMasterRepository.findByCategoryAndCode(cat, code.trim())
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/dependencies")
    public ResponseEntity<Map<String, AddressLookupOptionDto>> dependencies(
            @RequestParam String category,
            @RequestParam String code) {
        String cat = normalizedCategory(category);
        if (!CATEGORIES.contains(cat) || !StringUtils.hasText(code)) {
            return ResponseEntity.badRequest().build();
        }

        Optional<ReferenceMaster> selected = referenceMasterRepository.findByCategoryAndCode(cat, code.trim());
        if (selected.isEmpty()) return ResponseEntity.notFound().build();

        Map<String, String> extra = parseExtra(selected.get().getExtraJson());
        Map<String, AddressLookupOptionDto> out = new LinkedHashMap<>();
        putIfPresent(out, "country", "COUNTRY", extra.get("countryCode"));
        putIfPresent(out, "state", "STATE", extra.get("stateCode"));
        putIfPresent(out, "city", "CITY", extra.get("cityCode"));
        putIfPresent(out, "postal", "POSTAL_CODE", extra.get("postalCode"));
        putIfPresent(out, "region", "REGION", extra.get("regionCode"));
        putIfPresent(out, "zone", "ZONE", extra.get("zoneCode"));

        return ResponseEntity.ok(out);
    }

    private void putIfPresent(Map<String, AddressLookupOptionDto> out, String key, String category, String code) {
        if (!StringUtils.hasText(code)) return;
        referenceMasterRepository.findByCategoryAndCode(category, code.trim())
                .map(this::toDto)
                .ifPresent(v -> out.put(key, v));
    }

    private AddressLookupOptionDto toDto(ReferenceMaster row) {
        return AddressLookupOptionDto.builder()
                .category(row.getCategory())
                .code(row.getCode())
                .name(row.getName())
                .description(row.getDescription())
                .extra(parseExtra(row.getExtraJson()))
                .build();
    }

    private Map<String, String> parseExtra(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyMap();
        try {
            Map<String, Object> raw = objectMapper.readValue(json, new TypeReference<>() {});
            Map<String, String> out = new LinkedHashMap<>();
            raw.forEach((k, v) -> out.put(k, v == null ? "" : String.valueOf(v)));
            return out;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private String normalizedCategory(String category) {
        return category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
    }

    private String parentKey(String category) {
        return switch (category) {
            case "STATE" -> "countryCode";
            case "CITY" -> "stateCode";
            case "POSTAL_CODE" -> "cityCode";
            case "REGION" -> "countryCode";
            case "ZONE" -> "regionCode";
            default -> null;
        };
    }
}