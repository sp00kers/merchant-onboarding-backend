package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.merchantonboarding.dto.VerificationDTO;
import com.merchantonboarding.event.VerificationRequestEvent;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@ExtendWith(MockitoExtension.class)
class ExternalVerificationServiceTest {

    @Mock private VerificationResultRepository verificationResultRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private KafkaTemplate<String, VerificationRequestEvent> kafkaTemplate;

    @InjectMocks
    private ExternalVerificationService verificationService;

    private OnboardingCase testCase;
    private VerificationResult testVerification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verificationService, "verificationRequestTopic", "verification-request");

        testCase = new OnboardingCase();
        testCase.setCaseId("MOP-2026-001");
        testCase.setBusinessName("ABC Trading Sdn Bhd");
        testCase.setBusinessType("Sdn Bhd");
        testCase.setRegistrationNumber("123456789012");
        testCase.setBusinessAddress("No. 123, Jalan Kaya");
        testCase.setDirectorName("John Doe");
        testCase.setDirectorIC("900101141234");
        testCase.setDirectorPhone("0121234567");
        testCase.setDirectorEmail("john@gmail.com");
        testCase.setDocuments(new ArrayList<>());

        testVerification = new VerificationResult();
        testVerification.setId(1L);
        testVerification.setOnboardingCase(testCase);
        testVerification.setVerificationType("BUSINESS_REGISTRATION");
        testVerification.setStatus("PENDING");
        testVerification.setExternalReference("EXT-12345");
        testVerification.setVerifiedBy("System");
    }

    @SuppressWarnings("unchecked")
    private void mockKafkaSend() {
        SendResult<String, VerificationRequestEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("verification-request", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, VerificationRequestEvent>> future =
                CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(VerificationRequestEvent.class)))
                .thenReturn(future);
    }

    // ─── triggerVerification() ──────────────────────────────

    // Test: triggering a new verification creates a PENDING record in the database and publishes a Kafka message to the verification-request topic
    @Test
    void triggerVerification_NewVerification() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(verificationResultRepository.findByOnboardingCaseCaseIdAndVerificationType(
                "MOP-2026-001", "BUSINESS_REGISTRATION")).thenReturn(Optional.empty());
        when(verificationResultRepository.save(any(VerificationResult.class)))
                .thenAnswer(inv -> {
                    VerificationResult v = inv.getArgument(0);
                    v.setId(1L);
                    return v;
                });
        mockKafkaSend();

        VerificationDTO result = verificationService.triggerVerification(
                "MOP-2026-001", "BUSINESS_REGISTRATION");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("BUSINESS_REGISTRATION", result.getVerificationType());
        verify(kafkaTemplate).send(eq("verification-request"), eq("MOP-2026-001"), any());
    }

    // Test: if a verification is already PENDING, return the existing one without saving again or sending a duplicate Kafka message
    @Test
    void triggerVerification_ExistingPending() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(verificationResultRepository.findByOnboardingCaseCaseIdAndVerificationType(
                "MOP-2026-001", "BUSINESS_REGISTRATION")).thenReturn(Optional.of(testVerification));

        VerificationDTO result = verificationService.triggerVerification(
                "MOP-2026-001", "BUSINESS_REGISTRATION");

        assertEquals("PENDING", result.getStatus());
        // Should NOT save or publish again
        verify(verificationResultRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    // Test: re-triggering a previously completed (PASSED) verification resets it to PENDING and clears the old score
    @Test
    void triggerVerification_RetriggerCompleted() {
        testVerification.setStatus("PASSED");
        testVerification.setConfidenceScore(95);
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(verificationResultRepository.findByOnboardingCaseCaseIdAndVerificationType(
                "MOP-2026-001", "BUSINESS_REGISTRATION")).thenReturn(Optional.of(testVerification));
        when(verificationResultRepository.save(any(VerificationResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockKafkaSend();

        VerificationDTO result = verificationService.triggerVerification(
                "MOP-2026-001", "BUSINESS_REGISTRATION");

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getConfidenceScore());
        verify(verificationResultRepository).save(any());
    }

    // Test: triggering verification for a non-existent case throws ResourceNotFoundException (404)
    @Test
    void triggerVerification_CaseNotFound() {
        when(caseRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> verificationService.triggerVerification("NONEXISTENT", "BUSINESS_REGISTRATION"));
    }

    // ─── triggerAllVerifications() ──────────────────────────

    // Test: triggerAllVerifications triggers all 3 types (BUSINESS_REGISTRATION, DIRECTOR_ID, BENEFICIAL_OWNERSHIP) at once
    @Test
    void triggerAllVerifications_TriggersThreeTypes() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(verificationResultRepository.findByOnboardingCaseCaseIdAndVerificationType(
                eq("MOP-2026-001"), anyString())).thenReturn(Optional.empty());
        when(verificationResultRepository.save(any(VerificationResult.class)))
                .thenAnswer(inv -> {
                    VerificationResult v = inv.getArgument(0);
                    v.setId(1L);
                    return v;
                });
        mockKafkaSend();

        List<VerificationDTO> results = verificationService.triggerAllVerifications("MOP-2026-001");

        assertEquals(3, results.size());
    }

    // ─── getVerificationResults() ──────────────────────────

    // Test: retrieving verification results for a case returns the list of all verification records
    @Test
    void getVerificationResults_ReturnsList() {
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(testVerification));

        List<VerificationDTO> results = verificationService.getVerificationResults("MOP-2026-001");

        assertEquals(1, results.size());
        assertEquals("BUSINESS_REGISTRATION", results.get(0).getVerificationType());
    }

    // ─── getVerificationSummary() ──────────────────────────

    // Test: when all 3 verification types pass, the summary overall status is "ALL_PASSED" with 3 completed and 0 failed
    @Test
    void getVerificationSummary_AllPassed() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        VerificationResult v2 = createVerification("DIRECTOR_ID", "PASSED");
        VerificationResult v3 = createVerification("BENEFICIAL_OWNERSHIP", "PASSED");

        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1, v2, v3));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(95.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("ALL_PASSED", summary.getOverallStatus());
        assertEquals(3, summary.getCompletedCount());
        assertEquals(0, summary.getFailedCount());
    }

    // Test: when any verification type fails, the summary overall status is "ISSUES_FOUND"
    @Test
    void getVerificationSummary_IssuesFound() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        VerificationResult v2 = createVerification("DIRECTOR_ID", "FAILED");

        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1, v2));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(45.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("ISSUES_FOUND", summary.getOverallStatus());
        assertEquals(1, summary.getFailedCount());
    }

    // Test: when some verifications are still pending, the summary overall status is "IN_PROGRESS"
    @Test
    void getVerificationSummary_InProgress() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        VerificationResult v2 = createVerification("DIRECTOR_ID", "PENDING");

        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1, v2));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(null);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("IN_PROGRESS", summary.getOverallStatus());
    }

    // Test: when no verifications exist at all, the summary overall status is "NOT_STARTED"
    @Test
    void getVerificationSummary_NotStarted() {
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(Collections.emptyList());
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(null);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("NOT_STARTED", summary.getOverallStatus());
    }

    // ─── Recommendation thresholds ──────────────────────────

    // Test: average confidence score >= 90 results in "AUTO_APPROVE" recommendation (merchant is highly trustworthy)
    @Test
    void getVerificationSummary_AutoApprove() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(95.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("AUTO_APPROVE", summary.getRecommendation());
    }

    // Test: average confidence score 70-89 results in "MANUAL_REVIEW" recommendation (needs human review)
    @Test
    void getVerificationSummary_ManualReview() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(75.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("MANUAL_REVIEW", summary.getRecommendation());
    }

    // Test: average confidence score 50-69 results in "ENHANCED_DUE_DILIGENCE" recommendation (higher scrutiny needed)
    @Test
    void getVerificationSummary_EnhancedDueDiligence() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "PASSED");
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(55.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("ENHANCED_DUE_DILIGENCE", summary.getRecommendation());
    }

    // Test: average confidence score below 50 results in "REJECTION_RECOMMENDED" (merchant is too risky)
    @Test
    void getVerificationSummary_RejectionRecommended() {
        VerificationResult v1 = createVerification("BUSINESS_REGISTRATION", "FAILED");
        when(verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(v1));
        when(verificationResultRepository.getAverageConfidenceScoreByCaseId("MOP-2026-001"))
                .thenReturn(30.0);

        VerificationDTO.VerificationSummary summary =
                verificationService.getVerificationSummary("MOP-2026-001");

        assertEquals("REJECTION_RECOMMENDED", summary.getRecommendation());
    }

    // ─── Helper ──────────────────────────────────────────────

    private VerificationResult createVerification(String type, String status) {
        VerificationResult v = new VerificationResult();
        v.setId(System.nanoTime());
        v.setOnboardingCase(testCase);
        v.setVerificationType(type);
        v.setStatus(status);
        v.setVerifiedBy("System");
        return v;
    }
}
