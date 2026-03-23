package com.merchantonboarding.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.dto.RiskScoreDTO;
import com.merchantonboarding.dto.RiskScoreDTO.RiskFactor;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
@Transactional
public class RiskCalculationService {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    // Weight configuration (total = 100)
    private static final int WEIGHT_BUSINESS_TYPE = 20;
    private static final int WEIGHT_MERCHANT_CATEGORY = 25;
    private static final int WEIGHT_VERIFICATION = 30;
    private static final int WEIGHT_DOCUMENT = 15;
    private static final int WEIGHT_DATA_QUALITY = 10;

    // Risk thresholds
    private static final int THRESHOLD_LOW = 30;
    private static final int THRESHOLD_MEDIUM = 50;
    private static final int THRESHOLD_HIGH = 70;

    /**
     * Calculate risk score for a case
     */
    public RiskScoreDTO calculateRiskScore(String caseId) {
        OnboardingCase caseData = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        List<VerificationResult> verifications = verificationResultRepository
                .findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId);

        return calculateRiskScore(caseData, verifications);
    }

    /**
     * Calculate risk score with provided data
     */
    public RiskScoreDTO calculateRiskScore(OnboardingCase caseData, List<VerificationResult> verifications) {
        List<RiskFactor> factors = new ArrayList<>();
        int totalWeightedScore = 0;

        // Factor 1: Business Type Risk (20 points max)
        RiskFactor businessTypeFactor = calculateBusinessTypeRisk(caseData.getBusinessType());
        factors.add(businessTypeFactor);
        totalWeightedScore += businessTypeFactor.getScore();

        // Factor 2: Merchant Category Risk (25 points max)
        RiskFactor merchantCategoryFactor = calculateMerchantCategoryRisk(caseData.getMerchantCategory());
        factors.add(merchantCategoryFactor);
        totalWeightedScore += merchantCategoryFactor.getScore();

        // Factor 3: Verification Confidence Score (30 points max)
        RiskFactor verificationFactor = calculateVerificationRisk(verifications);
        factors.add(verificationFactor);
        totalWeightedScore += verificationFactor.getScore();

        // Factor 4: Document Completeness (15 points max)
        RiskFactor documentFactor = calculateDocumentRisk(caseData);
        factors.add(documentFactor);
        totalWeightedScore += documentFactor.getScore();

        // Factor 5: Data Quality (10 points max)
        RiskFactor dataQualityFactor = calculateDataQualityRisk(caseData);
        factors.add(dataQualityFactor);
        totalWeightedScore += dataQualityFactor.getScore();

        // Build result
        RiskScoreDTO result = new RiskScoreDTO();
        result.setTotalScore(totalWeightedScore);
        result.setRiskLevel(determineRiskLevel(totalWeightedScore));
        result.setRecommendation(determineRecommendation(totalWeightedScore));
        result.setFactors(factors);
        result.setCalculatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Update case with risk score
        caseData.setRiskScore(totalWeightedScore);
        caseData.setRiskLevel(result.getRiskLevel());
        caseRepository.save(caseData);

        return result;
    }

    private RiskFactor calculateBusinessTypeRisk(String businessType) {
        int score;
        String description;
        String impact;

        if (businessType == null) {
            score = 15; // High risk if unknown
            description = "Business type not specified";
            impact = "NEGATIVE";
        } else {
            switch (businessType.toLowerCase()) {
                case "bhd":
                case "public limited company":
                    score = 3; // Very low risk - public companies are heavily regulated
                    description = "Public limited company - highly regulated structure";
                    impact = "POSITIVE";
                    break;
                case "sdn bhd":
                case "private limited":
                    score = 6; // Low risk - established corporate structure
                    description = "Private limited company - established corporate structure";
                    impact = "POSITIVE";
                    break;
                case "partnership":
                    score = 10; // Medium risk
                    description = "Partnership - moderate regulatory oversight";
                    impact = "NEUTRAL";
                    break;
                case "sole proprietorship":
                case "sole proprietor":
                    score = 15; // Higher risk - less regulatory oversight
                    description = "Sole proprietorship - limited regulatory oversight";
                    impact = "NEGATIVE";
                    break;
                default:
                    score = 12;
                    description = "Unknown business type: " + businessType;
                    impact = "NEGATIVE";
            }
        }

        return new RiskFactor(
                "Business Type",
                "STRUCTURE",
                score,
                WEIGHT_BUSINESS_TYPE,
                WEIGHT_BUSINESS_TYPE,
                description,
                impact
        );
    }

    private RiskFactor calculateMerchantCategoryRisk(String merchantCategory) {
        int score;
        String description;
        String impact;

        if (merchantCategory == null) {
            score = 20;
            description = "Merchant category not specified";
            impact = "NEGATIVE";
        } else {
            String category = merchantCategory.toLowerCase();
            if (category.contains("retail") || category.contains("food") || category.contains("f&b")) {
                score = 5; // Low risk - traditional, well-understood business
                description = "Low-risk merchant category with established patterns";
                impact = "POSITIVE";
            } else if (category.contains("e-commerce") || category.contains("online")) {
                score = 15; // Medium-high risk - higher fraud potential
                description = "E-commerce category - elevated fraud monitoring required";
                impact = "NEGATIVE";
            } else if (category.contains("services") || category.contains("professional")) {
                score = 10; // Medium risk
                description = "Services category - moderate risk profile";
                impact = "NEUTRAL";
            } else if (category.contains("gaming") || category.contains("crypto") || category.contains("forex")) {
                score = 22; // High risk
                description = "High-risk merchant category - enhanced due diligence required";
                impact = "NEGATIVE";
            } else {
                score = 12;
                description = "Standard merchant category";
                impact = "NEUTRAL";
            }
        }

        return new RiskFactor(
                "Merchant Category",
                "BUSINESS",
                score,
                WEIGHT_MERCHANT_CATEGORY,
                WEIGHT_MERCHANT_CATEGORY,
                description,
                impact
        );
    }

    private RiskFactor calculateVerificationRisk(List<VerificationResult> verifications) {
        if (verifications == null || verifications.isEmpty()) {
            return new RiskFactor(
                    "Verification Score",
                    "VERIFICATION",
                    25, // High risk if no verifications
                    WEIGHT_VERIFICATION,
                    WEIGHT_VERIFICATION,
                    "No verification checks completed",
                    "NEGATIVE"
            );
        }

        // Calculate average confidence score from completed verifications
        List<VerificationResult> completed = verifications.stream()
                .filter(v -> "COMPLETED".equals(v.getStatus()) && v.getConfidenceScore() != null)
                .toList();

        if (completed.isEmpty()) {
            return new RiskFactor(
                    "Verification Score",
                    "VERIFICATION",
                    20,
                    WEIGHT_VERIFICATION,
                    WEIGHT_VERIFICATION,
                    "Verifications pending or failed",
                    "NEGATIVE"
            );
        }

        double avgConfidence = completed.stream()
                .mapToInt(VerificationResult::getConfidenceScore)
                .average()
                .orElse(0);

        // Invert confidence to risk score (high confidence = low risk)
        // confidence 90-100 -> risk 3-6
        // confidence 70-89 -> risk 9-15
        // confidence 50-69 -> risk 18-21
        // confidence <50 -> risk 24-30
        int riskScore;
        String description;
        String impact;

        if (avgConfidence >= 90) {
            riskScore = 3;
            description = String.format("High verification confidence (%.0f%%)", avgConfidence);
            impact = "POSITIVE";
        } else if (avgConfidence >= 70) {
            riskScore = 12;
            description = String.format("Moderate verification confidence (%.0f%%)", avgConfidence);
            impact = "NEUTRAL";
        } else if (avgConfidence >= 50) {
            riskScore = 21;
            description = String.format("Low verification confidence (%.0f%%) - additional checks recommended", avgConfidence);
            impact = "NEGATIVE";
        } else {
            riskScore = 27;
            description = String.format("Very low verification confidence (%.0f%%) - manual review required", avgConfidence);
            impact = "NEGATIVE";
        }

        // Check for any failed verifications
        long failedCount = verifications.stream().filter(v -> "FAILED".equals(v.getStatus())).count();
        if (failedCount > 0) {
            riskScore = Math.min(30, riskScore + 5);
            description += String.format(" (%d verification(s) failed)", failedCount);
        }

        return new RiskFactor(
                "Verification Score",
                "VERIFICATION",
                riskScore,
                WEIGHT_VERIFICATION,
                WEIGHT_VERIFICATION,
                description,
                impact
        );
    }

    private RiskFactor calculateDocumentRisk(OnboardingCase caseData) {
        int documentCount = caseData.getDocuments() != null ? caseData.getDocuments().size() : 0;
        int requiredDocuments = 3; // Business registration, Director ID, Financial statement

        if (documentCount >= requiredDocuments) {
            return new RiskFactor(
                    "Document Completeness",
                    "DOCUMENTATION",
                    3,
                    WEIGHT_DOCUMENT,
                    WEIGHT_DOCUMENT,
                    String.format("All required documents provided (%d/%d)", documentCount, requiredDocuments),
                    "POSITIVE"
            );
        } else if (documentCount > 0) {
            int score = 8 + ((requiredDocuments - documentCount) * 3);
            return new RiskFactor(
                    "Document Completeness",
                    "DOCUMENTATION",
                    Math.min(score, 12),
                    WEIGHT_DOCUMENT,
                    WEIGHT_DOCUMENT,
                    String.format("Incomplete documentation (%d/%d)", documentCount, requiredDocuments),
                    "NEGATIVE"
            );
        } else {
            return new RiskFactor(
                    "Document Completeness",
                    "DOCUMENTATION",
                    15,
                    WEIGHT_DOCUMENT,
                    WEIGHT_DOCUMENT,
                    "No documents uploaded",
                    "NEGATIVE"
            );
        }
    }

    private RiskFactor calculateDataQualityRisk(OnboardingCase caseData) {
        int completedFields = 0;
        int totalOptionalFields = 5;

        // Check optional/quality fields
        if (caseData.getBusinessAddress() != null && caseData.getBusinessAddress().length() > 20) completedFields++;
        if (caseData.getDirectorPhone() != null && !caseData.getDirectorPhone().isEmpty()) completedFields++;
        if (caseData.getDirectorEmail() != null && caseData.getDirectorEmail().contains("@")) completedFields++;
        if (caseData.getRegistrationNumber() != null && caseData.getRegistrationNumber().length() >= 8) completedFields++;
        if (caseData.getDirectorIC() != null && caseData.getDirectorIC().length() >= 10) completedFields++;

        double completionPercentage = (double) completedFields / totalOptionalFields * 100;
        int score;
        String description;
        String impact;

        if (completionPercentage >= 80) {
            score = 2;
            description = "High data quality - all fields properly completed";
            impact = "POSITIVE";
        } else if (completionPercentage >= 60) {
            score = 5;
            description = "Good data quality - most fields completed";
            impact = "NEUTRAL";
        } else if (completionPercentage >= 40) {
            score = 7;
            description = "Moderate data quality - some fields missing or incomplete";
            impact = "NEUTRAL";
        } else {
            score = 10;
            description = "Poor data quality - many fields missing or incomplete";
            impact = "NEGATIVE";
        }

        return new RiskFactor(
                "Data Quality",
                "DATA",
                score,
                WEIGHT_DATA_QUALITY,
                WEIGHT_DATA_QUALITY,
                description,
                impact
        );
    }

    private String determineRiskLevel(int totalScore) {
        if (totalScore <= THRESHOLD_LOW) {
            return "LOW";
        } else if (totalScore <= THRESHOLD_MEDIUM) {
            return "MEDIUM";
        } else if (totalScore <= THRESHOLD_HIGH) {
            return "HIGH";
        } else {
            return "CRITICAL";
        }
    }

    private String determineRecommendation(int totalScore) {
        if (totalScore <= THRESHOLD_LOW) {
            return "AUTO_APPROVE";
        } else if (totalScore <= THRESHOLD_MEDIUM) {
            return "MANUAL_REVIEW";
        } else if (totalScore <= THRESHOLD_HIGH) {
            return "ENHANCED_DUE_DILIGENCE";
        } else {
            return "REJECTION_RECOMMENDED";
        }
    }
}
