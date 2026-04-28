package com.tms.edi.controller.master;

import com.tms.edi.dto.master.MasterPartyDto;
import com.tms.edi.entity.master.MasterParty;
import com.tms.edi.repository.master.MasterPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master-parties")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MasterPartyController {

    private final MasterPartyRepository masterPartyRepository;

    @GetMapping
    public ResponseEntity<Page<MasterPartyDto>> list(
            @RequestParam String partyType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        if (!StringUtils.hasText(partyType)) {
            return ResponseEntity.badRequest().build();
        }
        String pt = partyType.trim().toUpperCase();
        Page<MasterParty> raw = activeOnly
                ? masterPartyRepository.findByPartyTypeAndIsActiveOrderByPartyCodeAsc(
                        pt, true, PageRequest.of(page, size, Sort.by("partyCode")))
                : masterPartyRepository.findByPartyTypeOrderByPartyCodeAsc(
                        pt, PageRequest.of(page, size, Sort.by("partyCode")));
        return ResponseEntity.ok(raw.map(this::toDto));
    }

    @GetMapping("/types")
    public List<String> partyTypes() {
        return List.of("CUSTOMER", "CARRIER", "SUPPLIER", "AGENT");
    }

    @GetMapping("/{id}")
    public ResponseEntity<MasterPartyDto> getOne(@PathVariable Long id) {
        return masterPartyRepository.findById(id)
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MasterPartyDto> create(@RequestBody MasterPartyDto body) {
        if (body == null || !StringUtils.hasText(body.getPartyType()) || !StringUtils.hasText(body.getPartyCode())
                || !StringUtils.hasText(body.getName())) {
            return ResponseEntity.badRequest().build();
        }
        String pt = body.getPartyType().trim().toUpperCase();
        String pc = body.getPartyCode().trim();
        if (masterPartyRepository.findByPartyTypeAndPartyCode(pt, pc).isPresent()) {
            return ResponseEntity.badRequest().build();
        }
        MasterParty e = MasterParty.builder()
                .partyType(pt)
                .partyCode(pc)
                .name(body.getName().trim())
                .legalName(body.getLegalName())
                .vatNumber(body.getVatNumber())
                .countryCode(body.getCountryCode())
                .city(body.getCity())
                .email(body.getEmail())
                .phone(body.getPhone())
                .isActive(body.getIsActive() != null ? body.getIsActive() : true)
                .build();
        return ResponseEntity.ok(toDto(masterPartyRepository.save(e)));
    }

    private MasterPartyDto toDto(MasterParty p) {
        return MasterPartyDto.builder()
                .id(p.getId())
                .partyType(p.getPartyType())
                .partyCode(p.getPartyCode())
                .name(p.getName())
                .legalName(p.getLegalName())
                .vatNumber(p.getVatNumber())
                .countryCode(p.getCountryCode())
                .city(p.getCity())
                .email(p.getEmail())
                .phone(p.getPhone())
                .isActive(p.getIsActive())
                .build();
    }
}