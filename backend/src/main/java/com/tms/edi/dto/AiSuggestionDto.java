package com.tms.edi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSuggestionDto {

    private List<SuggestionItem> suggestions;
    private Double overallConfidence;
    private List<String> unmappedRequired;

    @Data
    @Builder
    public static class SuggestionItem {
        private String targetField;
        private String sourcePath;
        private Double confidence;
        private String reason;
        private String suggestedTransform;
        private String sourceType;
        private String targetType;
    }
}
