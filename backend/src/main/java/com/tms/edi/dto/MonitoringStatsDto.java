package com.tms.edi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringStatsDto {

    private long totalFilesReceived;
    private long totalFilesProcessed;
    private long totalFilesFailed;
    private long totalFilesPending;
    private double successRatePercent;
    private double avgProcessingSeconds;
    private long openErrors;

    private List<PartnerVolume> partnerVolumes;
    private List<ErrorTypeCount> errorTypeCounts;
    private Map<String, Long> hourlyThroughput;

    @Data
    @Builder
    public static class PartnerVolume {
        private String partnerCode;
        private String partnerName;
        private long fileCount;
        private double percentage;
    }

    @Data
    @Builder
    public static class ErrorTypeCount {
        private String errorType;
        private String errorCode;
        private long count;
        private double percentage;
    }
}
