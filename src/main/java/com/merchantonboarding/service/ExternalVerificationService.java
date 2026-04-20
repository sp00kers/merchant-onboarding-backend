package com.merchantonboarding.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.dto.VerificationDTO;
import com.merchantonboarding.event.VerificationRequestEvent;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.Document;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
@Transactional
public class ExternalVerificationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalVerificationService.class);

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private KafkaTemplate<String, VerificationRequestEvent> kafkaTemplate;

    @Value("${app.kafka.topics.verification-request}")
    private String verificationRequestTopic;

    // Available verification types (aligned with uploaded background verification documents)
    public static final String TYPE_BUSINESS_REGISTRATION = "BUSINESS_REGISTRATION";
    public static final String TYPE_DIRECTOR_ID = "DIRECTOR_ID";
    public static final String TYPE_BENEFICIAL_OWNERSHIP = "BENEFICIAL_OWNERSHIP";

    public static final List<String> ALL_VERIFICATION_TYPES = Arrays.asList(
            TYPE_BUSINESS_REGISTRATION, TYPE_DIRECTOR_ID, TYPE_BENEFICIAL_OWNERSHIP
    );

    // Map verification type → uploaded document type name
    private static final Map<String, String> VERIFICATION_TO_DOC_TYPE = Map.of(
            TYPE_BUSINESS_REGISTRATION, "Business Registration Certificate",
            TYPE_DIRECTOR_ID, "Director Government ID",
            TYPE_BENEFICIAL_OWNERSHIP, "Beneficial Ownership Declaration"
    );

    /**
     * Trigger a specific verification type for a case
     */
    public VerificationDTO triggerVerification(String caseId, String verificationType) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        // Check if verification already exists
        Optional<VerificationResult> existing = verificationResultRepository
                .findByOnboardingCaseCaseIdAndVerificationType(caseId, verificationType);

        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            return convertToDTO(existing.get());
        }

        VerificationResult verification;
        if (existing.isPresent()) {
            // Re-trigger: reset existing record instead of creating a duplicate
            verification = existing.get();
            verification.setStatus("PENDING");
            verification.setConfidenceScore(null);
            verification.setResponseData(null);
            verification.setRiskIndicators(null);
            verification.setNotes(null);
            verification.setCompletedAt(null);
        } else {
            // Create new verification request
            verification = new VerificationResult();
            verification.setOnboardingCase(onboardingCase);
            verification.setVerificationType(verificationType);
            verification.setVerifiedBy("System");
        }
        verification.setStatus("PENDING");
        verification.setExternalReference("EXT-" + System.currentTimeMillis());

        VerificationResult saved = verificationResultRepository.save(verification);

        // Publish verification request to Kafka for external processing
        publishVerificationRequest(saved, onboardingCase);

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
        summary.setCompletedCount((int) results.stream().filter(v -> "PASSED".equals(v.getStatus())).count());
        summary.setPendingCount((int) results.stream().filter(v -> "PENDING".equals(v.getStatus()) || "IN_PROGRESS".equals(v.getStatus())).count());
        summary.setFailedCount((int) results.stream().filter(v -> "FAILED".equals(v.getStatus())).count());

        // Calculate average confidence score for recommendation
        Double avgScore = verificationResultRepository.getAverageConfidenceScoreByCaseId(caseId);

        // Determine overall status
        if (summary.getFailedCount() > 0) {
            summary.setOverallStatus("ISSUES_FOUND");
        } else if (summary.getPendingCount() > 0) {
            summary.setOverallStatus("IN_PROGRESS");
        } else if (summary.getCompletedCount() == ALL_VERIFICATION_TYPES.size()) {
            summary.setOverallStatus("ALL_PASSED");
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
     * Publish verification request event to Kafka topic.
     * The external mock API (or real verification services) will consume this event,
     * process the verification, and publish the result back to the response topic.
     */
    private void publishVerificationRequest(VerificationResult verification, OnboardingCase caseData) {
        // Look up the matching uploaded document filename
        String docTypeName = VERIFICATION_TO_DOC_TYPE.get(verification.getVerificationType());
        String documentFileName = caseData.getDocuments().stream()
                .filter(d -> docTypeName != null && docTypeName.equals(d.getType()))
                .map(Document::getName)
                .findFirst()
                .orElse(null);

        VerificationRequestEvent event = VerificationRequestEvent.builder()
                .caseId(caseData.getCaseId())
                .verificationType(verification.getVerificationType())
                .externalReference(verification.getExternalReference())
                .businessName(caseData.getBusinessName())
                .businessType(caseData.getBusinessType())
                .registrationNumber(caseData.getRegistrationNumber())
                .businessAddress(caseData.getBusinessAddress())
                .directorName(caseData.getDirectorName())
                .directorIC(caseData.getDirectorIC())
                .directorPhone(caseData.getDirectorPhone())
                .directorEmail(caseData.getDirectorEmail())
                .documentFileName(documentFileName)
                .requestedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(verificationRequestTopic, caseData.getCaseId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish verification request for case {} type {}: {}",
                                caseData.getCaseId(), verification.getVerificationType(), ex.getMessage());
                    } else {
                        log.info("Published verification request for case {} type {} to topic {} [partition={}, offset={}]",
                                caseData.getCaseId(), verification.getVerificationType(),
                                verificationRequestTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
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
}
