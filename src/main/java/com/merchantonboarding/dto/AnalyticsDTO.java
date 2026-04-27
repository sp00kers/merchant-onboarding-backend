package com.merchantonboarding.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AnalyticsDTO {
    // Overall Statistics
    private long totalCases;
    private long pendingCases;
    private long approvedCases;
    private long rejectedCases;
    private long inProgressCases;

    // Processing Stats
    private double averageProcessingTime; // in days
    private double approvalRate;
    private double rejectionRate;

    // Status Distribution
    private Map<String, Long> statusDistribution;

    // Time-based trends
    private List<TrendData> caseTrends;
    private List<TrendData> approvalTrends;

    // Category Distribution
    private Map<String, Long> merchantCategoryDistribution;
    private Map<String, Long> businessTypeDistribution;

    // Verification Stats
    private VerificationStats verificationStats;

    @Data
    public static class TrendData {
        private String period; // date or month
        private long totalCases;
        private long approvedCases;
        private long rejectedCases;
        private long pendingCases;
    }

    @Data
    public static class VerificationStats {
        private long totalVerifications;
        private long passedVerifications;
        private long failedVerifications;
        private double averageConfidenceScore;
        private Map<String, Long> verificationTypeDistribution;
    }
}
