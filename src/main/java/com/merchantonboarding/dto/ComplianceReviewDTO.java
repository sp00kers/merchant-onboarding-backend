package com.merchantonboarding.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ComplianceReviewDTO {
    private Long id;
    private String caseId;
    private String documentType;
    private String status;
    private String reason;
    private String externalReference;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String reviewedBy;

    @Data
    public static class ComplianceReviewSummary {
        private String caseId;
        private int totalReviews;
        private int passedCount;
        private int pendingCount;
        private int failedCount;
        private String overallStatus; // NOT_STARTED, IN_PROGRESS, ALL_PASSED, ISSUES_FOUND
    }
}
