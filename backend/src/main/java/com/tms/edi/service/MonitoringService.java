package com.tms.edi.service;

import com.tms.edi.dto.MonitoringStatsDto;
import com.tms.edi.enums.FileStatus;
import com.tms.edi.repository.EdiErrorLogRepository;
import com.tms.edi.repository.TmsFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final TmsFileRepository   tmsFileRepository;
    private final EdiErrorLogRepository errorLogRepository;

    public MonitoringStatsDto getStats() {
        long received   = tmsFileRepository.countByStatus(FileStatus.RECEIVED);
        long processing = tmsFileRepository.countByStatus(FileStatus.PROCESSING);
        long processed  = tmsFileRepository.countByStatus(FileStatus.PROCESSED);
        long error      = tmsFileRepository.countByStatus(FileStatus.ERROR);
        long pending    = tmsFileRepository.countByStatus(FileStatus.PENDING);
        long total      = received + processing + processed + error + pending;
        double rate     = total > 0 ? (processed * 100.0 / total) : 0;
        long openErrors = errorLogRepository.countByResolvedFlagFalse();

        List<MonitoringStatsDto.ErrorTypeCount> errorCounts = new ArrayList<>();
        List<Object[]> rawCounts = errorLogRepository.countByErrorTypeGrouped();
        long totalErrors = rawCounts.stream().mapToLong(r -> (Long) r[1]).sum();
        for (Object[] row : rawCounts) {
            errorCounts.add(MonitoringStatsDto.ErrorTypeCount.builder()
                    .errorType(row[0].toString())
                    .count((Long) row[1])
                    .percentage(totalErrors > 0 ? ((Long) row[1] * 100.0 / totalErrors) : 0)
                    .build());
        }

        return MonitoringStatsDto.builder()
                .totalFilesReceived(received + processed + error + pending)
                .totalFilesProcessed(processed)
                .totalFilesFailed(error)
                .totalFilesPending(pending)
                .successRatePercent(rate)
                .openErrors(openErrors)
                .errorTypeCounts(errorCounts)
                .build();
    }
}
