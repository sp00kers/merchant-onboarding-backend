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

    // Risk Distribution
    private Map<String, Long> riskDistribution; // LOW, MEDIUM, HIGH, CRITICAL

    // Status Distribution
    private Map<String, Long> statusDistribution;

    // Time-based trends
    private List<TrendData> caseTrends;
    private List<TrendData> approvalTrends;

    // Top Performers
    private List<UserPerformance> topReviewers;

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
    public static class UserPerformance {
        private String userId;
        private String userName;
        private long casesProcessed;
        private double averageProcessingTime;
        private double approvalRate;
    }

    @Data
    public static class VerificationStats {
        private long totalVerifications;
        private long completedVerifications;
        private long failedVerifications;
        private double averageConfidenceScore;
        private Map<String, Long> verificationTypeDistribution;
    }
}
