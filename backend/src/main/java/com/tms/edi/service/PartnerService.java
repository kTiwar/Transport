package com.tms.edi.service;

import com.tms.edi.dto.PartnerDto;
import com.tms.edi.entity.EdiPartner;
import com.tms.edi.exception.EdiException;
import com.tms.edi.repository.EdiPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final EdiPartnerRepository partnerRepository;

    public List<PartnerDto> findAll() {
        return partnerRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    public PartnerDto findById(Long id) {
        return toDto(partnerRepository.findById(id)
                .orElseThrow(() -> new EdiException("Partner not found: " + id)));
    }

    @Transactional
    public PartnerDto createPartner(PartnerDto dto) {
        if (partnerRepository.findByPartnerCode(dto.getPartnerCode()).isPresent()) {
            throw new EdiException("Partner code already exists: " + dto.getPartnerCode());
        }
        String apiKey = "pk_live_" + dto.getPartnerCode().toLowerCase()
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        EdiPartner partner = EdiPartner.builder()
                .partnerCode(dto.getPartnerCode())
                .partnerName(dto.getPartnerName())
                .defaultFormat(dto.getDefaultFormat())
                .processingMode(dto.getProcessingMode())
                .slaHours(dto.getSlaHours() != null ? dto.getSlaHours() : 24)
                .contactEmail(dto.getContactEmail())
                .sftpConfig(dto.getSftpConfig())
                .apiKey(apiKey)
                .active(true)
                .build();

        return toDto(partnerRepository.save(partner));
    }

    @Transactional
    public PartnerDto updatePartner(Long id, PartnerDto dto) {
        EdiPartner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new EdiException("Partner not found: " + id));
        partner.setPartnerName(dto.getPartnerName());
        partner.setDefaultFormat(dto.getDefaultFormat());
        partner.setProcessingMode(dto.getProcessingMode());
        partner.setSlaHours(dto.getSlaHours());
        partner.setContactEmail(dto.getContactEmail());
        partner.setSftpConfig(dto.getSftpConfig());
        if (dto.getActive() != null) partner.setActive(dto.getActive());
        return toDto(partnerRepository.save(partner));
    }

    private PartnerDto toDto(EdiPartner p) {
        return PartnerDto.builder()
                .partnerId(p.getPartnerId())
                .partnerCode(p.getPartnerCode())
                .partnerName(p.getPartnerName())
                .defaultFormat(p.getDefaultFormat())
                .processingMode(p.getProcessingMode())
                .active(p.getActive())
                .slaHours(p.getSlaHours())
                .contactEmail(p.getContactEmail())
                .sftpConfig(p.getSftpConfig())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
