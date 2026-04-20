package com.merchantonboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.merchantonboarding.model.VerificationResult;

@Repository
public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {

    List<VerificationResult> findByOnboardingCaseCaseIdOrderByRequestedAtDesc(String caseId);

    Optional<VerificationResult> findByOnboardingCaseCaseIdAndVerificationType(String caseId, String verificationType);

    List<VerificationResult> findByStatus(String status);

    @Query("SELECT v FROM VerificationResult v WHERE v.onboardingCase.caseId = :caseId AND v.status = 'PASSED'")
    List<VerificationResult> findCompletedVerificationsByCaseId(@Param("caseId") String caseId);

    @Query("SELECT AVG(v.confidenceScore) FROM VerificationResult v WHERE v.onboardingCase.caseId = :caseId AND v.status = 'PASSED'")
    Double getAverageConfidenceScoreByCaseId(@Param("caseId") String caseId);

    @Query("SELECT COUNT(v) FROM VerificationResult v WHERE v.onboardingCase.caseId = :caseId AND v.status = 'PENDING'")
    long countPendingVerificationsByCaseId(@Param("caseId") String caseId);

    @Query("SELECT COUNT(v) FROM VerificationResult v WHERE v.onboardingCase.caseId = :caseId AND v.status = 'PASSED'")
    long countCompletedVerificationsByCaseId(@Param("caseId") String caseId);
}
