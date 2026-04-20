package com.merchantonboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.merchantonboarding.model.ComplianceReviewResult;

@Repository
public interface ComplianceReviewResultRepository extends JpaRepository<ComplianceReviewResult, Long> {

    List<ComplianceReviewResult> findByOnboardingCaseCaseIdOrderByRequestedAtDesc(String caseId);

    Optional<ComplianceReviewResult> findByOnboardingCaseCaseIdAndDocumentType(String caseId, String documentType);
}
