package com.merchantonboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.RiskScoreDTO;
import com.merchantonboarding.service.RiskCalculationService;

@RestController
@RequestMapping("/api/risk")
public class RiskController {

    @Autowired
    private RiskCalculationService riskCalculationService;

    /**
     * Calculate risk score for a case
     */
    @PostMapping("/calculate/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<RiskScoreDTO> calculateRiskScore(@PathVariable String caseId) {
        RiskScoreDTO riskScore = riskCalculationService.calculateRiskScore(caseId);
        return ResponseEntity.ok(riskScore);
    }

    /**
     * Get current risk score for a case (without recalculating)
     */
    @GetMapping("/{caseId}")
    @PreAuthorize("hasAnyAuthority('CASE_VIEW', 'CASE_MANAGEMENT', 'BACKGROUND_CHECK', 'VERIFICATION_REPORTS', 'ALL_MODULES')")
    public ResponseEntity<RiskScoreDTO> getRiskScore(@PathVariable String caseId) {
        RiskScoreDTO riskScore = riskCalculationService.calculateRiskScore(caseId);
        return ResponseEntity.ok(riskScore);
    }
}
