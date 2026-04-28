package com.tms.edi.config;

import com.tms.edi.enums.FileStatus;
import com.tms.edi.enums.ProcessingMode;
import com.tms.edi.repository.TmsFileRepository;
import com.tms.edi.service.ProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Quartz-style scheduled processing job for SCHEDULED mode files.
 * Runs at 2 AM by default (configurable via cron expression).
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SchedulerConfig {

    private final TmsFileRepository  tmsFileRepository;
    private final ProcessingService  processingService;

    @Value("${tms.processing.scheduled-cron:0 0 2 * * ?}")
    private String scheduledCron;

    @Scheduled(cron = "${tms.processing.scheduled-cron:0 0 2 * * ?}")
    public void processScheduledFiles() {
        log.info("Scheduled processing job started");

        var from = OffsetDateTime.now().minusDays(1);
        var to   = OffsetDateTime.now();

        List<?> files = tmsFileRepository.findScheduledFiles(
                FileStatus.PENDING, ProcessingMode.SCHEDULED, from, to);

        log.info("Found {} SCHEDULED files to process", files.size());

        files.forEach(f -> {
            var tmsFile = (com.tms.edi.entity.TmsFile) f;
            try {
                processingService.processFile(tmsFile.getEntryNo(), null);
            } catch (Exception e) {
                log.error("Scheduled processing failed for entryNo={}: {}",
                        tmsFile.getEntryNo(), e.getMessage());
            }
        });

        log.info("Scheduled processing job completed");
    }

    @Scheduled(fixedRateString = "${tms.processing.retry-delay-ms:60000}")
    public void retryFailedFiles() {
        // Auto-retry files that haven't exceeded max retry count
        tmsFileRepository
            .findByStatusAndProcessingModeAndIsDeletedFalse(FileStatus.ERROR, ProcessingMode.AUTO)
            .stream()
            .filter(f -> f.getRetryCount() < 3)
            .forEach(f -> {
                log.info("Auto-retrying entryNo={} (attempt {})", f.getEntryNo(), f.getRetryCount() + 1);
                try {
                    processingService.processFile(f.getEntryNo(), null);
                } catch (Exception e) {
                    log.error("Auto-retry failed for entryNo={}: {}", f.getEntryNo(), e.getMessage());
                }
            });
    }
}
