package com.merchantonboarding.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class VerificationDTO {
    private Long id;
    private String caseId;
    private String verificationType;
    private String status;
    private Integer confidenceScore;
    private String externalReference;
    private String responseData;
    private String riskIndicators;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String verifiedBy;
    private String notes;

    @Data
    public static class VerificationSummary {
        private String caseId;
        private int totalVerifications;
        private int completedCount;
        private int pendingCount;
        private int failedCount;
        private String overallStatus;
        private String recommendation;
    }

    @Data
    public static class TriggerRequest {
        private String verificationType;
    }
}
