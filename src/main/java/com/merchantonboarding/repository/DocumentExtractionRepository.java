package com.merchantonboarding.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.merchantonboarding.model.DocumentExtraction;

@Repository
public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, Long> {

    Optional<DocumentExtraction> findByDocumentId(Long documentId);

    @Query("SELECT de FROM DocumentExtraction de WHERE de.document.onboardingCase.caseId = :caseId")
    List<DocumentExtraction> findByCaseId(@Param("caseId") String caseId);

    @Query("SELECT de FROM DocumentExtraction de WHERE de.validationStatus = :status")
    List<DocumentExtraction> findByValidationStatus(@Param("status") String status);

    @Query("SELECT AVG(de.confidenceScore) FROM DocumentExtraction de WHERE de.document.onboardingCase.caseId = :caseId")
    Double getAverageConfidenceScoreByCaseId(@Param("caseId") String caseId);
}
