package com.merchantonboarding.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merchantonboarding.dto.VerificationDTO;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
@Transactional
public class ExternalVerificationService {

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Random random = new Random();

    // Available verification types
    public static final String TYPE_BUSINESS_REGISTRY = "BUSINESS_REGISTRY";
    public static final String TYPE_IDENTITY_VERIFICATION = "IDENTITY_VERIFICATION";
    public static final String TYPE_ADDRESS_VERIFICATION = "ADDRESS_VERIFICATION";
    public static final String TYPE_FINANCIAL_CHECK = "FINANCIAL_CHECK";
    public static final String TYPE_SANCTIONS_SCREENING = "SANCTIONS_SCREENING";

    public static final List<String> ALL_VERIFICATION_TYPES = Arrays.asList(
            TYPE_BUSINESS_REGISTRY, TYPE_IDENTITY_VERIFICATION, TYPE_ADDRESS_VERIFICATION, TYPE_FINANCIAL_CHECK, TYPE_SANCTIONS_SCREENING
    );

    /**
     * Trigger a specific verification type for a case
     */
    public VerificationDTO triggerVerification(String caseId, String verificationType) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        // Check if verification already exists and is pending/in-progress
        Optional<VerificationResult> existing = verificationResultRepository
                .findByOnboardingCaseCaseIdAndVerificationType(caseId, verificationType);

        if (existing.isPresent() &&
            ("PENDING".equals(existing.get().getStatus()) || "IN_PROGRESS".equals(existing.get().getStatus()))) {
            return convertToDTO(existing.get());
        }

        // Create new verification request
        VerificationResult verification = new VerificationResult();
        verification.setOnboardingCase(onboardingCase);
        verification.setVerificationType(verificationType);
        verification.setStatus("PENDING");
        verification.setExternalReference("EXT-" + System.currentTimeMillis());
        verification.setVerifiedBy("System");

        VerificationResult saved = verificationResultRepository.save(verification);

        // Trigger async mock verification
        processVerificationAsync(saved.getId(), onboardingCase);

        return convertToDTO(saved);
    }

    /**
     * Trigger all verification types for a case
     */
    public List<VerificationDTO> triggerAllVerifications(String caseId) {
        return ALL_VERIFICATION_TYPES.stream()
                .map(type -> triggerVerification(caseId, type))
                .collect(Collectors.toList());
    }

    /**
     * Get all verification results for a case
     */
    public List<VerificationDTO> getVerificationResults(String caseId) {
        return verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get verification summary for a case
     */
    public VerificationDTO.VerificationSummary getVerificationSummary(String caseId) {
        List<VerificationResult> results = verificationResultRepository
                .findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId);

        VerificationDTO.VerificationSummary summary = new VerificationDTO.VerificationSummary();
        summary.setCaseId(caseId);
        summary.setTotalVerifications(results.size());
        summary.setCompletedCount((int) results.stream().filter(v -> "COMPLETED".equals(v.getStatus())).count());
        summary.setPendingCount((int) results.stream().filter(v -> "PENDING".equals(v.getStatus()) || "IN_PROGRESS".equals(v.getStatus())).count());
        summary.setFailedCount((int) results.stream().filter(v -> "FAILED".equals(v.getStatus())).count());

        // Calculate average confidence score
        Double avgScore = verificationResultRepository.getAverageConfidenceScoreByCaseId(caseId);
        summary.setAverageConfidenceScore(avgScore != null ? avgScore.intValue() : 0);

        // Determine overall status
        if (summary.getFailedCount() > 0) {
            summary.setOverallStatus("ISSUES_FOUND");
        } else if (summary.getPendingCount() > 0) {
            summary.setOverallStatus("IN_PROGRESS");
        } else if (summary.getCompletedCount() == ALL_VERIFICATION_TYPES.size()) {
            summary.setOverallStatus("COMPLETED");
        } else {
            summary.setOverallStatus("NOT_STARTED");
        }

        // Determine recommendation based on average score
        if (avgScore != null) {
            if (avgScore >= 90) {
                summary.setRecommendation("AUTO_APPROVE");
            } else if (avgScore >= 70) {
                summary.setRecommendation("MANUAL_REVIEW");
            } else if (avgScore >= 50) {
                summary.setRecommendation("ENHANCED_DUE_DILIGENCE");
            } else {
                summary.setRecommendation("REJECTION_RECOMMENDED");
            }
        } else {
            summary.setRecommendation("PENDING_VERIFICATION");
        }

        return summary;
    }

    /**
     * Process webhook response (for real external API integration)
     */
    public void processWebhookResponse(String externalReference, int confidenceScore, String status,
                                        String responseData, String riskIndicators) {
        // This would be called by a real external API webhook
        // For now, it's handled by the async mock process
    }

    /**
     * Async mock verification processing
     */
    @Async
    public CompletableFuture<Void> processVerificationAsync(Long verificationId, OnboardingCase caseData) {
        try {
            // Simulate API call delay (1-5 seconds)
            Thread.sleep(1000 + random.nextInt(4000));

            VerificationResult verification = verificationResultRepository.findById(verificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Verification not found"));

            // Update status to IN_PROGRESS
            verification.setStatus("IN_PROGRESS");
            verificationResultRepository.save(verification);

            // Simulate processing delay
            Thread.sleep(1000 + random.nextInt(2000));

            // Generate mock verification result based on type
            MockVerificationResult mockResult = generateMockResult(verification.getVerificationType(), caseData);

            verification.setStatus(mockResult.isSuccess() ? "COMPLETED" : "FAILED");
            verification.setConfidenceScore(mockResult.getConfidenceScore());
            verification.setResponseData(mockResult.getResponseData());
            verification.setRiskIndicators(mockResult.getRiskIndicators());
            verification.setCompletedAt(LocalDateTime.now());
            verification.setNotes(mockResult.getNotes());

            verificationResultRepository.save(verification);

            // Send notification
            if (notificationService != null && caseData.getAssignedTo() != null) {
                notificationService.notifyVerificationComplete(
                        caseData.getCaseId(),
                        caseData.getBusinessName(),
                        verification.getVerificationType(),
                        mockResult.getConfidenceScore(),
                        getAssignedUserId(caseData.getAssignedTo())
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error processing verification: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    private MockVerificationResult generateMockResult(String verificationType, OnboardingCase caseData) {
        MockVerificationResult result = new MockVerificationResult();
        Map<String, Object> responseData = new HashMap<>();
        List<String> riskIndicators = new ArrayList<>();

        // Base confidence score with some randomness
        int baseScore = 70 + random.nextInt(25); // 70-94 base

        switch (verificationType) {
            case TYPE_BUSINESS_REGISTRY:
                responseData.put("registryMatch", true);
                responseData.put("businessName", caseData.getBusinessName());
                responseData.put("registrationNumber", caseData.getRegistrationNumber());
                responseData.put("registrationStatus", "ACTIVE");
                responseData.put("incorporationDate", "2018-03-15");

                // Adjust score based on business type
                if ("Bhd".equals(caseData.getBusinessType())) {
                    baseScore += 5; // Public companies generally more trustworthy
                } else if ("Sole Proprietorship".equals(caseData.getBusinessType())) {
                    baseScore -= 5;
                    riskIndicators.add("Higher risk business structure");
                }

                result.setNotes("Business registry verification completed successfully");
                break;

            case TYPE_IDENTITY_VERIFICATION:
                responseData.put("identityVerified", true);
                responseData.put("directorName", caseData.getDirectorName());
                responseData.put("icNumber", caseData.getDirectorIC());
                responseData.put("watchlistCheck", "CLEAR");
                responseData.put("pepStatus", random.nextBoolean() ? "NOT_PEP" : "POTENTIAL_PEP");

                if ("POTENTIAL_PEP".equals(responseData.get("pepStatus"))) {
                    baseScore -= 10;
                    riskIndicators.add("Potential Politically Exposed Person (PEP)");
                }

                result.setNotes("Identity verification completed");
                break;

            case TYPE_ADDRESS_VERIFICATION:
                responseData.put("addressVerified", true);
                responseData.put("address", caseData.getBusinessAddress());
                responseData.put("addressType", "COMMERCIAL");
                responseData.put("geocodeConfidence", "HIGH");

                if (caseData.getBusinessAddress() == null || caseData.getBusinessAddress().length() < 20) {
                    baseScore -= 15;
                    riskIndicators.add("Incomplete address information");
                }

                result.setNotes("Address verification completed");
                break;

            case TYPE_FINANCIAL_CHECK:
                responseData.put("creditScore", 650 + random.nextInt(150));
                responseData.put("bankruptcyCheck", "CLEAR");
                responseData.put("outstandingLitigation", random.nextInt(10) < 2);
                responseData.put("estimatedRevenue", "RM " + (100000 + random.nextInt(900000)));

                if ((Boolean) responseData.get("outstandingLitigation")) {
                    baseScore -= 20;
                    riskIndicators.add("Outstanding litigation detected");
                }

                result.setNotes("Financial check completed");
                break;

            case TYPE_SANCTIONS_SCREENING:
                responseData.put("sanctionsMatch", false);
                responseData.put("screenedLists", List.of("UN Sanctions", "OFAC SDN", "EU Sanctions", "BNM Sanctions"));
                responseData.put("businessName", caseData.getBusinessName());
                responseData.put("directorName", caseData.getDirectorName());
                responseData.put("matchScore", 0);

                boolean potentialMatch = random.nextInt(10) < 1; // 10% chance of potential match
                if (potentialMatch) {
                    responseData.put("sanctionsMatch", true);
                    responseData.put("matchScore", 60 + random.nextInt(30));
                    baseScore -= 30;
                    riskIndicators.add("Potential sanctions list match detected");
                    riskIndicators.add("Manual review required for sanctions clearance");
                }

                result.setNotes("Sanctions screening completed");
                break;
        }

        // Add some random variation
        baseScore = Math.max(30, Math.min(100, baseScore + random.nextInt(11) - 5));

        result.setConfidenceScore(baseScore);
        result.setSuccess(baseScore >= 50);

        try {
            result.setResponseData(objectMapper.writeValueAsString(responseData));
            result.setRiskIndicators(objectMapper.writeValueAsString(riskIndicators));
        } catch (Exception e) {
            result.setResponseData("{}");
            result.setRiskIndicators("[]");
        }

        return result;
    }

    private String getAssignedUserId(String assignedToName) {
        // This would look up the user ID from the name
        // For now, return null as notifications will handle it
        return null;
    }

    private VerificationDTO convertToDTO(VerificationResult v) {
        VerificationDTO dto = new VerificationDTO();
        dto.setId(v.getId());
        dto.setCaseId(v.getOnboardingCase() != null ? v.getOnboardingCase().getCaseId() : null);
        dto.setVerificationType(v.getVerificationType());
        dto.setStatus(v.getStatus());
        dto.setConfidenceScore(v.getConfidenceScore());
        dto.setExternalReference(v.getExternalReference());
        dto.setResponseData(v.getResponseData());
        dto.setRiskIndicators(v.getRiskIndicators());
        dto.setRequestedAt(v.getRequestedAt());
        dto.setCompletedAt(v.getCompletedAt());
        dto.setVerifiedBy(v.getVerifiedBy());
        dto.setNotes(v.getNotes());
        return dto;
    }

    // Inner class for mock results
    private static class MockVerificationResult {
        private int confidenceScore;
        private boolean success;
        private String responseData;
        private String riskIndicators;
        private String notes;

        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getResponseData() { return responseData; }
        public void setResponseData(String responseData) { this.responseData = responseData; }
        public String getRiskIndicators() { return riskIndicators; }
        public void setRiskIndicators(String riskIndicators) { this.riskIndicators = riskIndicators; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
