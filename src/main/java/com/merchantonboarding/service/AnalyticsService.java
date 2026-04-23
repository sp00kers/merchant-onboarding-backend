package com.merchantonboarding.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.dto.AnalyticsDTO;
import com.merchantonboarding.model.ComplianceReviewResult;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.ComplianceReviewResultRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired(required = false)
    private VerificationResultRepository verificationResultRepository;

    @Autowired(required = false)
    private ComplianceReviewResultRepository complianceReviewResultRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get comprehensive dashboard analytics
     */
    public AnalyticsDTO getDashboardAnalytics() {
        AnalyticsDTO analytics = new AnalyticsDTO();

        List<OnboardingCase> allCases = caseRepository.findAll();

        // Overall Statistics
        analytics.setTotalCases(allCases.size());
        analytics.setPendingCases(countByStatus(allCases, "Pending Review", "Compliance Review", "Background Verification"));
        analytics.setApprovedCases(countByStatus(allCases, "Approved"));
        analytics.setRejectedCases(countByStatus(allCases, "Rejected"));
        analytics.setInProgressCases(countByStatus(allCases, "Compliance Review", "Background Verification"));

        // Processing Stats
        analytics.setAverageProcessingTime(calculateAverageProcessingTime(allCases));
        analytics.setApprovalRate(calculateRate(analytics.getApprovedCases(), allCases.size()));
        analytics.setRejectionRate(calculateRate(analytics.getRejectedCases(), allCases.size()));

        // Status Distribution
        analytics.setStatusDistribution(calculateStatusDistribution(allCases));

        // Category Distributions
        analytics.setMerchantCategoryDistribution(calculateMerchantCategoryDistribution(allCases));
        analytics.setBusinessTypeDistribution(calculateBusinessTypeDistribution(allCases));

        // Trends (last 30 days)
        analytics.setCaseTrends(calculateCaseTrends(allCases, 30));
        analytics.setApprovalTrends(calculateApprovalTrends(allCases, 30));

        // Verification Stats (Background Verification + Compliance Review)
        analytics.setVerificationStats(calculateVerificationStats());

        return analytics;
    }

    /**
     * Get analytics filtered by date range
     */
    public AnalyticsDTO getAnalyticsByDateRange(String startDate, String endDate) {
        AnalyticsDTO analytics = getDashboardAnalytics();

        // Filter cases by date range if needed
        LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

        List<OnboardingCase> filteredCases = caseRepository.findAll().stream()
                .filter(c -> {
                    if (c.getCreatedDate() == null) return false;
                    try {
                        LocalDate caseDate = LocalDate.parse(c.getCreatedDate(), DATE_FORMATTER);
                        return !caseDate.isBefore(start) && !caseDate.isAfter(end);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        // Recalculate with filtered data
        analytics.setTotalCases(filteredCases.size());
        analytics.setPendingCases(countByStatus(filteredCases, "Pending Review", "Compliance Review", "Background Verification"));
        analytics.setApprovedCases(countByStatus(filteredCases, "Approved"));
        analytics.setRejectedCases(countByStatus(filteredCases, "Rejected"));

        return analytics;
    }

    private long countByStatus(List<OnboardingCase> cases, String... statuses) {
        Set<String> statusSet = new HashSet<>(Arrays.asList(statuses));
        return cases.stream()
                .filter(c -> c.getStatus() != null && statusSet.contains(c.getStatus()))
                .count();
    }

    private double calculateAverageProcessingTime(List<OnboardingCase> cases) {
        List<OnboardingCase> completedCases = cases.stream()
                .filter(c -> "Approved".equals(c.getStatus()) || "Rejected".equals(c.getStatus()))
                .filter(c -> c.getCreatedDate() != null && c.getLastUpdated() != null)
                .collect(Collectors.toList());

        if (completedCases.isEmpty()) {
            return 0.0;
        }

        double totalDays = completedCases.stream()
                .mapToLong(c -> {
                    try {
                        LocalDate created = LocalDate.parse(c.getCreatedDate(), DATE_FORMATTER);
                        LocalDate updated = LocalDate.parse(c.getLastUpdated().substring(0, 10), DATE_FORMATTER);
                        return ChronoUnit.DAYS.between(created, updated);
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .sum();

        return Math.round((totalDays / completedCases.size()) * 100.0) / 100.0;
    }

    private double calculateRate(long count, long total) {
        if (total == 0) return 0.0;
        return Math.round((count * 100.0 / total) * 100.0) / 100.0;
    }

    private Map<String, Long> calculateStatusDistribution(List<OnboardingCase> cases) {
        return cases.stream()
                .filter(c -> c.getStatus() != null)
                .collect(Collectors.groupingBy(
                        OnboardingCase::getStatus,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> calculateMerchantCategoryDistribution(List<OnboardingCase> cases) {
        return cases.stream()
                .filter(c -> c.getMerchantCategory() != null)
                .collect(Collectors.groupingBy(
                        OnboardingCase::getMerchantCategory,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private Map<String, Long> calculateBusinessTypeDistribution(List<OnboardingCase> cases) {
        return cases.stream()
                .filter(c -> c.getBusinessType() != null)
                .collect(Collectors.groupingBy(
                        OnboardingCase::getBusinessType,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
    }

    private List<AnalyticsDTO.TrendData> calculateCaseTrends(List<OnboardingCase> cases, int days) {
        List<AnalyticsDTO.TrendData> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Group cases by creation date for total/pending counts ("cases submitted that day")
        Map<String, List<OnboardingCase>> casesByCreatedDate = cases.stream()
                .filter(c -> c.getCreatedDate() != null)
                .collect(Collectors.groupingBy(OnboardingCase::getCreatedDate));

        // Group approved/rejected cases by the date their status last changed (lastUpdated)
        Map<String, List<OnboardingCase>> approvedByDate = cases.stream()
                .filter(c -> "Approved".equals(c.getStatus()) && c.getLastUpdated() != null)
                .collect(Collectors.groupingBy(c -> c.getLastUpdated().substring(0, 10)));

        Map<String, List<OnboardingCase>> rejectedByDate = cases.stream()
                .filter(c -> "Rejected".equals(c.getStatus()) && c.getLastUpdated() != null)
                .collect(Collectors.groupingBy(c -> c.getLastUpdated().substring(0, 10)));

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);

            AnalyticsDTO.TrendData trend = new AnalyticsDTO.TrendData();
            trend.setPeriod(dateStr);

            List<OnboardingCase> submittedCases = casesByCreatedDate.getOrDefault(dateStr, Collections.emptyList());
            long approvedCount = approvedByDate.getOrDefault(dateStr, Collections.emptyList()).size();
            long rejectedCount = rejectedByDate.getOrDefault(dateStr, Collections.emptyList()).size();

            trend.setTotalCases(submittedCases.size());
            trend.setApprovedCases(approvedCount);
            trend.setRejectedCases(rejectedCount);
            trend.setPendingCases(submittedCases.stream()
                    .filter(c -> !"Approved".equals(c.getStatus()) && !"Rejected".equals(c.getStatus()))
                    .count());

            trends.add(trend);
        }

        return trends;
    }

    private List<AnalyticsDTO.TrendData> calculateApprovalTrends(List<OnboardingCase> cases, int days) {
        // Similar to case trends but focusing on approval/rejection rates
        return calculateCaseTrends(cases, days);
    }

    private AnalyticsDTO.VerificationStats calculateVerificationStats() {
        AnalyticsDTO.VerificationStats stats = new AnalyticsDTO.VerificationStats();

        long totalCount = 0;
        long passedCount = 0;
        long failedCount = 0;

        // Background Verification results
        if (verificationResultRepository != null) {
            List<VerificationResult> bgResults = verificationResultRepository.findAll();
            totalCount += bgResults.size();
            passedCount += bgResults.stream()
                    .filter(v -> "PASSED".equals(v.getStatus()))
                    .count();
            failedCount += bgResults.stream()
                    .filter(v -> "FAILED".equals(v.getStatus()))
                    .count();

            double avgConfidence = bgResults.stream()
                    .filter(v -> v.getConfidenceScore() != null)
                    .mapToInt(VerificationResult::getConfidenceScore)
                    .average()
                    .orElse(0.0);
            stats.setAverageConfidenceScore(Math.round(avgConfidence * 100.0) / 100.0);

            stats.setVerificationTypeDistribution(bgResults.stream()
                    .filter(v -> v.getVerificationType() != null)
                    .collect(Collectors.groupingBy(
                            VerificationResult::getVerificationType,
                            Collectors.counting()
                    )));
        }

        // Compliance Review results
        if (complianceReviewResultRepository != null) {
            List<ComplianceReviewResult> crResults = complianceReviewResultRepository.findAll();
            totalCount += crResults.size();
            passedCount += crResults.stream()
                    .filter(r -> "PASSED".equals(r.getStatus()))
                    .count();
            failedCount += crResults.stream()
                    .filter(r -> "FAILED".equals(r.getStatus()))
                    .count();
        }

        stats.setTotalVerifications(totalCount);
        stats.setPassedVerifications(passedCount);
        stats.setFailedVerifications(failedCount);

        return stats;
    }
}
