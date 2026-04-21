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

import com.merchantonboarding.dto.ComplianceReviewDTO;
import com.merchantonboarding.event.ComplianceRequestEvent;
import com.merchantonboarding.exception.ResourceNotFoundException;
import com.merchantonboarding.model.ComplianceReviewResult;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.ComplianceReviewResultRepository;

@ExtendWith(MockitoExtension.class)
class ComplianceReviewServiceTest {

    @Mock private ComplianceReviewResultRepository complianceReviewResultRepository;
    @Mock private CaseRepository caseRepository;
    @Mock private KafkaTemplate<String, ComplianceRequestEvent> kafkaTemplate;

    @InjectMocks
    private ComplianceReviewService complianceReviewService;

    private OnboardingCase testCase;
    private ComplianceReviewResult testReview;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(complianceReviewService, "complianceRequestTopic", "compliance-request");

        testCase = new OnboardingCase();
        testCase.setCaseId("MOP-2026-001");
        testCase.setBusinessName("ABC Trading Sdn Bhd");
        testCase.setBusinessType("Sdn Bhd");
        testCase.setRegistrationNumber("123456789012");
        testCase.setBusinessAddress("No. 123, Jalan Kaya");
        testCase.setDirectorName("John Doe");
        testCase.setDocuments(new ArrayList<>());

        testReview = new ComplianceReviewResult();
        testReview.setId(1L);
        testReview.setOnboardingCase(testCase);
        testReview.setDocumentType("BUSINESS_LICENSE");
        testReview.setStatus("PENDING");
        testReview.setExternalReference("CMP-12345");
        testReview.setReviewedBy("System");
    }

    @SuppressWarnings("unchecked")
    private void mockKafkaSend() {
        SendResult<String, ComplianceRequestEvent> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("compliance-request", 0), 0, 0, 0, 0, 0);
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, ComplianceRequestEvent>> future =
                CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(anyString(), anyString(), any(ComplianceRequestEvent.class)))
                .thenReturn(future);
    }

    // ─── triggerReview() ──────────────────────────────────

    @Test
    void triggerReview_NewReview() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdAndDocumentType(
                "MOP-2026-001", "BUSINESS_LICENSE")).thenReturn(Optional.empty());
        when(complianceReviewResultRepository.save(any(ComplianceReviewResult.class)))
                .thenAnswer(inv -> {
                    ComplianceReviewResult r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });
        mockKafkaSend();

        ComplianceReviewDTO result = complianceReviewService.triggerReview(
                "MOP-2026-001", "BUSINESS_LICENSE");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("BUSINESS_LICENSE", result.getDocumentType());
        verify(kafkaTemplate).send(eq("compliance-request"), eq("MOP-2026-001"), any());
    }

    @Test
    void triggerReview_ExistingPending() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdAndDocumentType(
                "MOP-2026-001", "BUSINESS_LICENSE")).thenReturn(Optional.of(testReview));

        ComplianceReviewDTO result = complianceReviewService.triggerReview(
                "MOP-2026-001", "BUSINESS_LICENSE");

        assertEquals("PENDING", result.getStatus());
        verify(complianceReviewResultRepository, never()).save(any());
    }

    @Test
    void triggerReview_RetriggerFailed() {
        testReview.setStatus("FAILED");
        testReview.setReason("Document expired");
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdAndDocumentType(
                "MOP-2026-001", "BUSINESS_LICENSE")).thenReturn(Optional.of(testReview));
        when(complianceReviewResultRepository.save(any(ComplianceReviewResult.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        mockKafkaSend();

        ComplianceReviewDTO result = complianceReviewService.triggerReview(
                "MOP-2026-001", "BUSINESS_LICENSE");

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getReason());
        verify(complianceReviewResultRepository).save(any());
    }

    @Test
    void triggerReview_CaseNotFound() {
        when(caseRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> complianceReviewService.triggerReview("NONEXISTENT", "BUSINESS_LICENSE"));
    }

    // ─── triggerAllReviews() ──────────────────────────────

    @Test
    void triggerAllReviews_TriggersThreeTypes() {
        when(caseRepository.findById("MOP-2026-001")).thenReturn(Optional.of(testCase));
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdAndDocumentType(
                eq("MOP-2026-001"), anyString())).thenReturn(Optional.empty());
        when(complianceReviewResultRepository.save(any(ComplianceReviewResult.class)))
                .thenAnswer(inv -> {
                    ComplianceReviewResult r = inv.getArgument(0);
                    r.setId(1L);
                    return r;
                });
        mockKafkaSend();

        List<ComplianceReviewDTO> results = complianceReviewService.triggerAllReviews("MOP-2026-001");

        assertEquals(3, results.size());
    }

    // ─── getReviewResults() ──────────────────────────────

    @Test
    void getReviewResults_ReturnsList() {
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(testReview));

        List<ComplianceReviewDTO> results = complianceReviewService.getReviewResults("MOP-2026-001");

        assertEquals(1, results.size());
        assertEquals("BUSINESS_LICENSE", results.get(0).getDocumentType());
    }

    // ─── getReviewSummary() ──────────────────────────────

    @Test
    void getReviewSummary_AllPassed() {
        ComplianceReviewResult r1 = createReview("BUSINESS_LICENSE", "PASSED");
        ComplianceReviewResult r2 = createReview("PCI_DSS_SAQ", "PASSED");
        ComplianceReviewResult r3 = createReview("TERMS_OF_SERVICE", "PASSED");

        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(r1, r2, r3));

        ComplianceReviewDTO.ComplianceReviewSummary summary =
                complianceReviewService.getReviewSummary("MOP-2026-001");

        assertEquals("ALL_PASSED", summary.getOverallStatus());
        assertEquals(3, summary.getPassedCount());
        assertEquals(0, summary.getFailedCount());
    }

    @Test
    void getReviewSummary_IssuesFound() {
        ComplianceReviewResult r1 = createReview("BUSINESS_LICENSE", "PASSED");
        ComplianceReviewResult r2 = createReview("PCI_DSS_SAQ", "FAILED");

        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(r1, r2));

        ComplianceReviewDTO.ComplianceReviewSummary summary =
                complianceReviewService.getReviewSummary("MOP-2026-001");

        assertEquals("ISSUES_FOUND", summary.getOverallStatus());
    }

    @Test
    void getReviewSummary_InProgress() {
        ComplianceReviewResult r1 = createReview("BUSINESS_LICENSE", "PASSED");
        ComplianceReviewResult r2 = createReview("PCI_DSS_SAQ", "PENDING");

        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(List.of(r1, r2));

        ComplianceReviewDTO.ComplianceReviewSummary summary =
                complianceReviewService.getReviewSummary("MOP-2026-001");

        assertEquals("IN_PROGRESS", summary.getOverallStatus());
    }

    @Test
    void getReviewSummary_NotStarted() {
        when(complianceReviewResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc("MOP-2026-001"))
                .thenReturn(Collections.emptyList());

        ComplianceReviewDTO.ComplianceReviewSummary summary =
                complianceReviewService.getReviewSummary("MOP-2026-001");

        assertEquals("NOT_STARTED", summary.getOverallStatus());
    }

    // ─── Helper ──────────────────────────────────────────

    private ComplianceReviewResult createReview(String docType, String status) {
        ComplianceReviewResult r = new ComplianceReviewResult();
        r.setId(System.nanoTime());
        r.setOnboardingCase(testCase);
        r.setDocumentType(docType);
        r.setStatus(status);
        r.setReviewedBy("System");
        return r;
    }
}
