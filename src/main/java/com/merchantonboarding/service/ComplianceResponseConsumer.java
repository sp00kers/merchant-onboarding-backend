package com.merchantonboarding.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.event.ComplianceResponseEvent;
import com.merchantonboarding.model.ComplianceReviewResult;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.repository.ComplianceReviewResultRepository;

@Service
public class ComplianceResponseConsumer {

        private static final Logger log = LoggerFactory.getLogger(ComplianceResponseConsumer.class);

        @Autowired
        private ComplianceReviewResultRepository complianceReviewResultRepository;

        @Autowired(required = false)
        private NotificationService notificationService;

        @KafkaListener(
                topics = "${app.kafka.topics.compliance-response}",
                containerFactory = "complianceKafkaListenerContainerFactory"
        )
        @Transactional
        public void handleComplianceResponse(ComplianceResponseEvent event) {
                log.info("Received compliance response for case {} type {} status {}",
                        event.getCaseId(), event.getDocumentType(), event.getStatus());

                try {
                Optional<ComplianceReviewResult> optResult = complianceReviewResultRepository
                        .findByOnboardingCaseCaseIdAndDocumentType(
                                event.getCaseId(), event.getDocumentType());

                if (optResult.isEmpty()) {
                        log.warn("No compliance review record found for case {} type {} — ignoring response",
                                event.getCaseId(), event.getDocumentType());
                        return;
                }

                ComplianceReviewResult review = optResult.get();
                review.setStatus(event.getStatus());
                review.setReason(event.getReason());
                review.setCompletedAt(event.getCompletedAt());

                complianceReviewResultRepository.save(review);

                log.info("Updated compliance review id={} for case {} type {} to status={}",
                        review.getId(), event.getCaseId(), event.getDocumentType(), event.getStatus());

                OnboardingCase onboardingCase = review.getOnboardingCase();
                if (notificationService != null && onboardingCase != null
                        && onboardingCase.getAssignedTo() != null) {
                        String title = "Compliance Review Complete";
                        String message = String.format("Compliance review (%s) for case %s ('%s') completed: %s.",
                                event.getDocumentType(), event.getCaseId(), onboardingCase.getBusinessName(), event.getStatus());
                        notificationService.notifyUser(
                                onboardingCase.getAssignedTo(), title, message,
                                "PASSED".equals(event.getStatus()) ? "SUCCESS" : "WARNING",
                                "COMPLIANCE", "Case", event.getCaseId(), true);
                }
                } catch (Exception e) {
                log.error("Error processing compliance response for case {} type {}: {}",
                        event.getCaseId(), event.getDocumentType(), e.getMessage(), e);
                }
        }
}
