package com.tms.edi.controller.master;

import com.tms.edi.dto.master.ReferenceMasterDto;
import com.tms.edi.entity.master.ReferenceMaster;
import com.tms.edi.repository.master.ReferenceMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reference-master")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReferenceMasterController {

    private final ReferenceMasterRepository referenceMasterRepository;

    @GetMapping("/categories")
    public List<String> categories() {
        return List.of(
                "CURRENCY",
                "INCOTERM",
                "TRANSPORT_MODE",
                "SERVICE_LEVEL",
                "EQUIPMENT_TYPE",
                "LOCATION_TYPE",
                "CHARGE_TYPE",
                "DOCUMENT_TYPE",
                "UOM",
                "COUNTRY",
                "STATE",
                "CITY",
                "POSTAL_CODE",
                "ADDRESS_TYPE",
                "REGION",
                "ZONE"
        );
    }

    @GetMapping
    public ResponseEntity<Page<ReferenceMasterDto>> list(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        if (!StringUtils.hasText(category)) {
            return ResponseEntity.badRequest().build();
        }
        String cat = category.trim().toUpperCase();
        Sort sort = Sort.by(Order.asc("sortOrder"), Order.asc("code"));
        Page<ReferenceMaster> raw = activeOnly
                ? referenceMasterRepository.findByCategoryAndIsActiveOrderBySortOrderAscCodeAsc(
                        cat, true, PageRequest.of(page, size, sort))
                : referenceMasterRepository.findByCategoryOrderBySortOrderAscCodeAsc(
                        cat, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(raw.map(this::toDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferenceMasterDto> getOne(@PathVariable Long id) {
        return referenceMasterRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ReferenceMasterDto> create(@RequestBody ReferenceMasterDto body) {
        if (body == null || !StringUtils.hasText(body.getCategory()) || !StringUtils.hasText(body.getCode())
                || !StringUtils.hasText(body.getName())) {
            return ResponseEntity.badRequest().build();
        }
        String cat = body.getCategory().trim().toUpperCase();
        String code = body.getCode().trim();
        if (referenceMasterRepository.findByCategoryAndCode(cat, code).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        ReferenceMaster e = ReferenceMaster.builder()
                .category(cat)
                .code(code)
                .name(body.getName().trim())
                .description(body.getDescription())
                .extraJson(body.getExtraJson())
                .sortOrder(body.getSortOrder())
                .isActive(body.getIsActive() != null ? body.getIsActive() : true)
                .build();
        return ResponseEntity.ok(toDto(referenceMasterRepository.save(e)));
    }

    private ReferenceMasterDto toDto(ReferenceMaster r) {
        return ReferenceMasterDto.builder()
                .id(r.getId())
                .category(r.getCategory())
                .code(r.getCode())
                .name(r.getName())
                .description(r.getDescription())
                .extraJson(r.getExtraJson())
                .sortOrder(r.getSortOrder())
                .isActive(r.getIsActive())
                .build();
    }
}