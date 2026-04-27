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

import com.merchantonboarding.dto.ComplianceReviewDTO;
import com.merchantonboarding.event.ComplianceRequestEvent;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.ComplianceReviewResult;
import com.merchantonboarding.model.Document;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.ComplianceReviewResultRepository;

@Service
@Transactional
public class ComplianceReviewService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReviewService.class);

    @Autowired
    private ComplianceReviewResultRepository complianceReviewResultRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private KafkaTemplate<String, ComplianceRequestEvent> kafkaTemplate;

    @Value("${app.kafka.topics.compliance-request}")
    private String complianceRequestTopic;

    public static final String TYPE_BUSINESS_LICENSE = "BUSINESS_LICENSE";
    public static final String TYPE_PCI_DSS_SAQ = "PCI_DSS_SAQ";
    public static final String TYPE_TERMS_OF_SERVICE = "TERMS_OF_SERVICE";

    public static final List<String> ALL_COMPLIANCE_TYPES = Arrays.asList(
            TYPE_BUSINESS_LICENSE, TYPE_PCI_DSS_SAQ, TYPE_TERMS_OF_SERVICE
    );

    // Map compliance type to uploaded document type name
    private static final Map<String, String> COMPLIANCE_TO_DOC_TYPE = Map.of(
            TYPE_BUSINESS_LICENSE, "Business License",
            TYPE_PCI_DSS_SAQ, "PCI DSS SAQ",
            TYPE_TERMS_OF_SERVICE, "Terms of Service"
    );

    public ComplianceReviewDTO triggerReview(String caseId, String documentType) {
        OnboardingCase onboardingCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        Optional<ComplianceReviewResult> existing = complianceReviewResultRepository
                .findByOnboardingCaseCaseIdAndDocumentType(caseId, documentType);

        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            return convertToDTO(existing.get());
        }

        ComplianceReviewResult review;
        if (existing.isPresent()) {
            // Re-trigger: reset existing record
            review = existing.get();
            review.setStatus("PENDING");
            review.setReason(null);
            review.setCompletedAt(null);
        } else {
            review = new ComplianceReviewResult();
            review.setOnboardingCase(onboardingCase);
            review.setDocumentType(documentType);
            review.setStatus("PENDING");
        }
        review.setExternalReference("CMP-" + System.currentTimeMillis());
        review.setReviewedBy("System");

        ComplianceReviewResult saved = complianceReviewResultRepository.save(review);

        publishComplianceRequest(saved, onboardingCase);

        return convertToDTO(saved);
    }

    public List<ComplianceReviewDTO> triggerAllReviews(String caseId) {
        return ALL_COMPLIANCE_TYPES.stream()
                .map(type -> triggerReview(caseId, type))
                .collect(Collectors.toList());
    }

    public List<ComplianceReviewDTO> getReviewResults(String caseId) {
        return complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ComplianceReviewDTO.ComplianceReviewSummary getReviewSummary(String caseId) {
        List<ComplianceReviewResult> results = complianceReviewResultRepository
                .findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId);

        ComplianceReviewDTO.ComplianceReviewSummary summary = new ComplianceReviewDTO.ComplianceReviewSummary();
        summary.setCaseId(caseId);
        summary.setTotalReviews(results.size());
        summary.setPassedCount((int) results.stream().filter(r -> "PASSED".equals(r.getStatus())).count());
        summary.setPendingCount((int) results.stream().filter(r -> "PENDING".equals(r.getStatus())).count());
        summary.setFailedCount((int) results.stream().filter(r -> "FAILED".equals(r.getStatus())).count());

        if (summary.getFailedCount() > 0) {
            summary.setOverallStatus("ISSUES_FOUND");
        } else if (summary.getPendingCount() > 0) {
            summary.setOverallStatus("IN_PROGRESS");
        } else if (summary.getPassedCount() == ALL_COMPLIANCE_TYPES.size()) {
            summary.setOverallStatus("ALL_PASSED");
        } else {
            summary.setOverallStatus("NOT_STARTED");
        }

        return summary;
    }

    private void publishComplianceRequest(ComplianceReviewResult review, OnboardingCase caseData) {
        // Look up the matching uploaded document filename and file path
        String docTypeName = COMPLIANCE_TO_DOC_TYPE.get(review.getDocumentType());
        Document matchedDoc = caseData.getDocuments().stream()
                .filter(d -> docTypeName != null && docTypeName.equals(d.getType()))
                .findFirst()
                .orElse(null);
        String documentFileName = matchedDoc != null ? matchedDoc.getName() : null;
        String documentFilePath = matchedDoc != null ? matchedDoc.getFilePath() : null;

        ComplianceRequestEvent event = ComplianceRequestEvent.builder()
                .caseId(caseData.getCaseId())
                .documentType(review.getDocumentType())
                .externalReference(review.getExternalReference())
                .businessName(caseData.getBusinessName())
                .businessType(caseData.getBusinessType())
                .registrationNumber(caseData.getRegistrationNumber())
                .businessAddress(caseData.getBusinessAddress())
                .directorName(caseData.getDirectorName())
                .documentFileName(documentFileName)
                .documentFilePath(documentFilePath)
                .requestedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(complianceRequestTopic, caseData.getCaseId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish compliance request for case {} type {}: {}",
                                caseData.getCaseId(), review.getDocumentType(), ex.getMessage());
                    } else {
                        log.info("Published compliance request for case {} type {} to topic {} [partition={}, offset={}]",
                                caseData.getCaseId(), review.getDocumentType(),
                                complianceRequestTopic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private ComplianceReviewDTO convertToDTO(ComplianceReviewResult r) {
        ComplianceReviewDTO dto = new ComplianceReviewDTO();
        dto.setId(r.getId());
        dto.setCaseId(r.getOnboardingCase() != null ? r.getOnboardingCase().getCaseId() : null);
        dto.setDocumentType(r.getDocumentType());
        dto.setStatus(r.getStatus());
        dto.setReason(r.getReason());
        dto.setExternalReference(r.getExternalReference());
        dto.setRequestedAt(r.getRequestedAt());
        dto.setCompletedAt(r.getCompletedAt());
        dto.setReviewedBy(r.getReviewedBy());
        return dto;
    }
}
