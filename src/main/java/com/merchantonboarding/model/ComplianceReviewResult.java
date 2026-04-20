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
@Table(name = "compliance_review_results")
@Data
public class ComplianceReviewResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private OnboardingCase onboardingCase;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // BUSINESS_LICENSE, PCI_DSS_SAQ, TERMS_OF_SERVICE

    @Column(nullable = false, length = 20)
    private String status; // PENDING, PASSED, FAILED

    @Column(length = 500)
    private String reason;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;
}
