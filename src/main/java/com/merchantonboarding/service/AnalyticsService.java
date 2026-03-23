package com.merchantonboarding.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired(required = false)
    private VerificationResultRepository verificationResultRepository;

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

        // Risk Distribution
        analytics.setRiskDistribution(calculateRiskDistribution(allCases));

        // Status Distribution
        analytics.setStatusDistribution(calculateStatusDistribution(allCases));

        // Category Distributions
        analytics.setMerchantCategoryDistribution(calculateMerchantCategoryDistribution(allCases));
        analytics.setBusinessTypeDistribution(calculateBusinessTypeDistribution(allCases));

        // Trends (last 30 days)
        analytics.setCaseTrends(calculateCaseTrends(allCases, 30));
        analytics.setApprovalTrends(calculateApprovalTrends(allCases, 30));

        // Top Performers
        analytics.setTopReviewers(calculateTopPerformers(allCases));

        // Verification Stats
        if (verificationResultRepository != null) {
            analytics.setVerificationStats(calculateVerificationStats());
        }

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
        // Calculate average time from creation to approval/rejection
        List<OnboardingCase> completedCases = cases.stream()
                .filter(c -> "Approved".equals(c.getStatus()) || "Rejected".equals(c.getStatus()))
                .filter(c -> c.getCreatedDate() != null && c.getLastUpdated() != null)
                .collect(Collectors.toList());

        if (completedCases.isEmpty()) {
            return 0.0;
        }

        // For demo purposes, return a mock average (real implementation would calculate from dates)
        return 3.5; // Average 3.5 days
    }

    private double calculateRate(long count, long total) {
        if (total == 0) return 0.0;
        return Math.round((count * 100.0 / total) * 100.0) / 100.0;
    }

    private Map<String, Long> calculateRiskDistribution(List<OnboardingCase> cases) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("LOW", cases.stream().filter(c -> "LOW".equals(c.getRiskLevel())).count());
        distribution.put("MEDIUM", cases.stream().filter(c -> "MEDIUM".equals(c.getRiskLevel())).count());
        distribution.put("HIGH", cases.stream().filter(c -> "HIGH".equals(c.getRiskLevel())).count());
        distribution.put("CRITICAL", cases.stream().filter(c -> "CRITICAL".equals(c.getRiskLevel())).count());
        distribution.put("UNASSESSED", cases.stream().filter(c -> c.getRiskLevel() == null).count());
        return distribution;
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

        // Group cases by creation date
        Map<String, List<OnboardingCase>> casesByDate = cases.stream()
                .filter(c -> c.getCreatedDate() != null)
                .collect(Collectors.groupingBy(OnboardingCase::getCreatedDate));

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String dateStr = date.format(DATE_FORMATTER);

            AnalyticsDTO.TrendData trend = new AnalyticsDTO.TrendData();
            trend.setPeriod(dateStr);

            List<OnboardingCase> dayCases = casesByDate.getOrDefault(dateStr, Collections.emptyList());
            trend.setTotalCases(dayCases.size());
            trend.setApprovedCases(dayCases.stream().filter(c -> "Approved".equals(c.getStatus())).count());
            trend.setRejectedCases(dayCases.stream().filter(c -> "Rejected".equals(c.getStatus())).count());
            trend.setPendingCases(dayCases.stream().filter(c -> !"Approved".equals(c.getStatus()) && !"Rejected".equals(c.getStatus())).count());

            trends.add(trend);
        }

        return trends;
    }

    private List<AnalyticsDTO.TrendData> calculateApprovalTrends(List<OnboardingCase> cases, int days) {
        // Similar to case trends but focusing on approval/rejection rates
        return calculateCaseTrends(cases, days);
    }

    private List<AnalyticsDTO.UserPerformance> calculateTopPerformers(List<OnboardingCase> cases) {
        // Group by assigned reviewer
        Map<String, List<OnboardingCase>> casesByReviewer = cases.stream()
                .filter(c -> c.getAssignedTo() != null && !c.getAssignedTo().isEmpty())
                .collect(Collectors.groupingBy(OnboardingCase::getAssignedTo));

        return casesByReviewer.entrySet().stream()
                .map(entry -> {
                    AnalyticsDTO.UserPerformance perf = new AnalyticsDTO.UserPerformance();
                    perf.setUserName(entry.getKey());
                    perf.setCasesProcessed(entry.getValue().size());
                    perf.setAverageProcessingTime(2.5 + Math.random() * 3); // Mock data

                    long approved = entry.getValue().stream()
                            .filter(c -> "Approved".equals(c.getStatus()))
                            .count();
                    long completed = entry.getValue().stream()
                            .filter(c -> "Approved".equals(c.getStatus()) || "Rejected".equals(c.getStatus()))
                            .count();
                    perf.setApprovalRate(completed > 0 ? (approved * 100.0 / completed) : 0);

                    return perf;
                })
                .sorted((a, b) -> Long.compare(b.getCasesProcessed(), a.getCasesProcessed()))
                .limit(5)
                .collect(Collectors.toList());
    }

    private AnalyticsDTO.VerificationStats calculateVerificationStats() {
        AnalyticsDTO.VerificationStats stats = new AnalyticsDTO.VerificationStats();

        if (verificationResultRepository == null) {
            return stats;
        }

        List<VerificationResult> results = verificationResultRepository.findAll();

        stats.setTotalVerifications(results.size());
        stats.setCompletedVerifications(results.stream()
                .filter(v -> "COMPLETED".equals(v.getStatus()))
                .count());
        stats.setFailedVerifications(results.stream()
                .filter(v -> "FAILED".equals(v.getStatus()))
                .count());

        double avgConfidence = results.stream()
                .filter(v -> v.getConfidenceScore() != null)
                .mapToInt(VerificationResult::getConfidenceScore)
                .average()
                .orElse(0.0);
        stats.setAverageConfidenceScore(Math.round(avgConfidence * 100.0) / 100.0);

        stats.setVerificationTypeDistribution(results.stream()
                .filter(v -> v.getVerificationType() != null)
                .collect(Collectors.groupingBy(
                        VerificationResult::getVerificationType,
                        Collectors.counting()
                )));

        return stats;
    }
}
