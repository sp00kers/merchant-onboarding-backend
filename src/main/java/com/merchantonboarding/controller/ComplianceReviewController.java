package com.merchantonboarding.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.ComplianceReviewDTO;
import com.merchantonboarding.service.ComplianceReviewService;

@RestController
@RequestMapping("/api/compliance")
public class ComplianceReviewController {

    @Autowired
    private ComplianceReviewService complianceReviewService;

    @PostMapping("/trigger/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'COMPLIANCE_CHECK', 'RISK_ASSESSMENT', 'ALL_MODULES')")
    public ResponseEntity<List<ComplianceReviewDTO>> triggerAllReviews(@PathVariable String caseId) {
        List<ComplianceReviewDTO> results = complianceReviewService.triggerAllReviews(caseId);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/trigger/{caseId}/{documentType}")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'COMPLIANCE_CHECK', 'RISK_ASSESSMENT', 'ALL_MODULES')")
    public ResponseEntity<ComplianceReviewDTO> triggerReview(
            @PathVariable String caseId, @PathVariable String documentType) {
        ComplianceReviewDTO result = complianceReviewService.triggerReview(caseId, documentType);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'COMPLIANCE_CHECK', 'RISK_ASSESSMENT', 'ALL_MODULES')")
    public ResponseEntity<List<ComplianceReviewDTO>> getReviewResults(@PathVariable String caseId) {
        List<ComplianceReviewDTO> results = complianceReviewService.getReviewResults(caseId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{caseId}/summary")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'COMPLIANCE_CHECK', 'RISK_ASSESSMENT', 'ALL_MODULES')")
    public ResponseEntity<ComplianceReviewDTO.ComplianceReviewSummary> getReviewSummary(@PathVariable String caseId) {
        ComplianceReviewDTO.ComplianceReviewSummary summary = complianceReviewService.getReviewSummary(caseId);
        return ResponseEntity.ok(summary);
    }
}
