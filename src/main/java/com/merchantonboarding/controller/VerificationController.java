package com.merchantonboarding.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.VerificationDTO;
import com.merchantonboarding.service.ExternalVerificationService;

@RestController
@RequestMapping("/api/verification")
public class VerificationController {

    @Autowired
    private ExternalVerificationService verificationService;

    /**
     * Trigger all verification types for a case
     */
    @PostMapping("/trigger/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<List<VerificationDTO>> triggerAllVerifications(@PathVariable String caseId) {
        List<VerificationDTO> results = verificationService.triggerAllVerifications(caseId);
        return ResponseEntity.ok(results);
    }

    /**
     * Trigger a specific verification type for a case
     */
    @PostMapping("/trigger/{caseId}/{verificationType}")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<VerificationDTO> triggerVerification(
            @PathVariable String caseId,
            @PathVariable String verificationType) {
        VerificationDTO result = verificationService.triggerVerification(caseId, verificationType);
        return ResponseEntity.ok(result);
    }

    /**
     * Get all verification results for a case
     */
    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<List<VerificationDTO>> getVerificationResults(@PathVariable String caseId) {
        List<VerificationDTO> results = verificationService.getVerificationResults(caseId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get verification summary for a case
     */
    @GetMapping("/{caseId}/summary")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<VerificationDTO.VerificationSummary> getVerificationSummary(@PathVariable String caseId) {
        VerificationDTO.VerificationSummary summary = verificationService.getVerificationSummary(caseId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get available verification types
     */
    @GetMapping("/types")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'ALL_MODULES')")
    public ResponseEntity<List<Map<String, String>>> getVerificationTypes() {
        List<Map<String, String>> types = List.of(
            Map.of("code", "BUSINESS_REGISTRY", "name", "Business Registry", "description", "Verify business registration with SSM"),
            Map.of("code", "IDENTITY_VERIFICATION", "name", "Identity Verification", "description", "Verify director identity and conduct watchlist check"),
            Map.of("code", "ADDRESS_VERIFICATION", "name", "Address Verification", "description", "Verify business address existence"),
            Map.of("code", "FINANCIAL_CHECK", "name", "Financial Check", "description", "Credit score and litigation check"),
            Map.of("code", "SANCTIONS_SCREENING", "name", "Sanctions Screening", "description", "Screen against global sanctions and watchlists")
        );
        return ResponseEntity.ok(types);
    }

    /**
     * Webhook endpoint (legacy) — verification responses are now handled via Kafka consumer.
     * This endpoint is retained for potential non-Kafka fallback or manual testing.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveWebhook(@RequestBody Map<String, Object> payload) {
        System.out.println("Received webhook (legacy endpoint): " + payload);
        return ResponseEntity.ok().build();
    }
}
