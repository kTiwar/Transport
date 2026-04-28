package com.tms.edi.service;

import com.tms.edi.dto.FileResponseDto;
import com.tms.edi.entity.EdiPartner;
import com.tms.edi.entity.TmsFile;
import com.tms.edi.enums.FileStatus;
import com.tms.edi.enums.FileType;
import com.tms.edi.enums.ProcessingMode;
import com.tms.edi.exception.EdiException;
import com.tms.edi.kafka.FileEventProducer;
import com.tms.edi.repository.EdiPartnerRepository;
import com.tms.edi.repository.TmsFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final TmsFileRepository    tmsFileRepository;
    private final EdiPartnerRepository partnerRepository;
    private final FileStorageService   fileStorageService;
    private final FileEventProducer    fileEventProducer;

    // ── Upload ────────────────────────────────────────────────────────────────

    @Transactional
    public FileResponseDto uploadFile(MultipartFile multipartFile,
                                      Long partnerId,
                                      ProcessingMode processingMode) throws Exception {
        EdiPartner partner = partnerRepository.findById(partnerId)
                .orElseThrow(() -> new EdiException("Partner not found: " + partnerId));

        byte[] content = multipartFile.getBytes();
        FileStorageService.StorageResult stored =
                fileStorageService.store(partner.getPartnerCode(),
                        multipartFile.getOriginalFilename(), content);

        if (tmsFileRepository.existsByChecksumAndPartner_PartnerIdAndIsDeletedFalse(stored.checksum(), partnerId)) {
            throw new EdiException(
                    "Duplicate file for this partner: identical content was already uploaded. "
                    + "Delete the previous file or use a different file, then try again.");
        }

        FileType fileType = detectFileType(multipartFile.getOriginalFilename());
        TmsFile tmsFile = TmsFile.builder()
                .fileName(multipartFile.getOriginalFilename())
                .fileType(fileType)
                .partner(partner)
                .receivedTimestamp(OffsetDateTime.now())
                .processingMode(processingMode)
                .status(processingMode == ProcessingMode.AUTO ? FileStatus.RECEIVED : FileStatus.PENDING)
                .fileSize(stored.fileSize())
                .checksum(stored.checksum())
                .storagePath(stored.storagePath())
                .build();

        TmsFile saved = tmsFileRepository.save(tmsFile);
        log.info("Stored file {} for partner {} (mode={})", saved.getEntryNo(),
                partner.getPartnerCode(), processingMode);

        if (processingMode == ProcessingMode.AUTO) {
            fileEventProducer.publishReceived(saved.getEntryNo());
        }

        return toDto(saved);
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    public Page<FileResponseDto> listFiles(int page, int size, String sortField, String sortDir) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return tmsFileRepository.findByIsDeletedFalseOrderByReceivedTimestampDesc(pageable)
                .map(this::toDto);
    }

    public FileResponseDto getFile(Long entryNo) {
        TmsFile f = tmsFileRepository.findByEntryNoAndIsDeletedFalse(entryNo)
                .orElseThrow(() -> new EdiException("File not found: " + entryNo));
        return toDto(f);
    }

    public byte[] downloadFile(Long entryNo) throws Exception {
        TmsFile f = tmsFileRepository.findByEntryNoAndIsDeletedFalse(entryNo)
                .orElseThrow(() -> new EdiException("File not found: " + entryNo));
        if (f.getFileContent() != null) return f.getFileContent();
        return fileStorageService.load(f.getStoragePath());
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    @Transactional
    public void softDelete(Long entryNo) {
        TmsFile f = tmsFileRepository.findByEntryNoAndIsDeletedFalse(entryNo)
                .orElseThrow(() -> new EdiException("File not found: " + entryNo));
        f.setIsDeleted(true);
        f.setStatus(FileStatus.DELETED);
        tmsFileRepository.save(f);
    }

    @Transactional
    public void resetForRetry(Long entryNo) {
        TmsFile f = tmsFileRepository.findByEntryNoAndIsDeletedFalse(entryNo)
                .orElseThrow(() -> new EdiException("File not found: " + entryNo));
        f.setStatus(FileStatus.RECEIVED);
        f.setErrorMessage(null);
        tmsFileRepository.save(f);
        fileEventProducer.publishReceived(entryNo);
        log.info("File {} queued for retry", entryNo);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private FileType detectFileType(String fileName) {
        if (fileName == null) return FileType.TXT;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".xml"))            return FileType.XML;
        if (lower.endsWith(".json"))           return FileType.JSON;
        if (lower.endsWith(".csv"))            return FileType.CSV;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return FileType.EXCEL;
        if (lower.endsWith(".edi") || lower.contains("edifact")) return FileType.EDIFACT;
        if (lower.endsWith(".x12"))            return FileType.X12;
        return FileType.TXT;
    }

    public FileResponseDto toDto(TmsFile f) {
        return FileResponseDto.builder()
                .entryNo(f.getEntryNo())
                .fileName(f.getFileName())
                .fileType(f.getFileType())
                .partnerId(f.getPartner().getPartnerId())
                .partnerCode(f.getPartner().getPartnerCode())
                .partnerName(f.getPartner().getPartnerName())
                .status(f.getStatus())
                .processingMode(f.getProcessingMode())
                .fileSize(f.getFileSize())
                .checksum(f.getChecksum())
                .storagePath(f.getStoragePath())
                .errorMessage(f.getErrorMessage())
                .retryCount(f.getRetryCount())
                .orderCount(f.getOrderCount())
                .receivedTimestamp(f.getReceivedTimestamp())
                .processedTimestamp(f.getProcessedTimestamp())
                .build();
    }
}
