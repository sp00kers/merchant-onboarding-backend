package com.merchantonboarding.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "verification_results")
@Data
public class VerificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private OnboardingCase onboardingCase;

    @Column(name = "verification_type", nullable = false, length = 50)
    private String verificationType; // BUSINESS_REGISTRY, IDENTITY_VERIFICATION, ADDRESS_VERIFICATION, FINANCIAL_CHECK, SANCTIONS_SCREENING

    @Column(nullable = false, length = 20)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "confidence_score")
    private Integer confidenceScore; // 0-100

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData; // JSON response from external API

    @Column(name = "risk_indicators", columnDefinition = "TEXT")
    private String riskIndicators; // JSON array of risk indicators found

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy; // System or User ID

    @Column(length = 500)
    private String notes;
}
