package com.merchantonboarding.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.event.VerificationResponseEvent;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.VerificationResultRepository;

/**
 * Kafka consumer that listens for verification response events from external
 * verification services (Bank Negara, SSM, etc.) and updates the database accordingly.
 */
@Service
public class VerificationResponseConsumer {

    private static final Logger log = LoggerFactory.getLogger(VerificationResponseConsumer.class);

    @Autowired
    private VerificationResultRepository verificationResultRepository;

    @Autowired(required = false)
    private NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.verification-response}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void handleVerificationResponse(VerificationResponseEvent event) {
        log.info("Received verification response for case {} type {} status {}",
                event.getCaseId(), event.getVerificationType(), event.getStatus());

        try {
            // Look up the verification record by caseId and verificationType
            Optional<VerificationResult> optResult = verificationResultRepository
                    .findByOnboardingCaseCaseIdAndVerificationType(
                            event.getCaseId(), event.getVerificationType());

            if (optResult.isEmpty()) {
                log.warn("No verification record found for case {} type {} — ignoring response",
                        event.getCaseId(), event.getVerificationType());
                return;
            }

            VerificationResult verification = optResult.get();

            // Update the verification result with the response data
            verification.setStatus(event.getStatus());
            verification.setConfidenceScore(event.getConfidenceScore());
            verification.setResponseData(event.getResponseData());
            verification.setRiskIndicators(event.getRiskIndicators());
            verification.setCompletedAt(event.getCompletedAt());
            verification.setNotes(event.getNotes());

            verificationResultRepository.save(verification);

            log.info("Updated verification result id={} for case {} type {} to status={} score={}",
                    verification.getId(), event.getCaseId(), event.getVerificationType(),
                    event.getStatus(), event.getConfidenceScore());

            // Send real-time notification to the assigned reviewer
            OnboardingCase onboardingCase = verification.getOnboardingCase();
            if (notificationService != null && onboardingCase != null
                    && onboardingCase.getAssignedTo() != null) {
                notificationService.notifyVerificationComplete(
                        event.getCaseId(),
                        onboardingCase.getBusinessName(),
                        event.getVerificationType(),
                        event.getConfidenceScore(),
                        onboardingCase.getAssignedTo()
                );
            }
        } catch (Exception e) {
            log.error("Error processing verification response for case {} type {}: {}",
                    event.getCaseId(), event.getVerificationType(), e.getMessage(), e);
        }
    }
}
