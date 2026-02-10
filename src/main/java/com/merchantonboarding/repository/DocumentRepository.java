package com.merchantonboarding.repository;

import com.merchantonboarding.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    // Find documents by case ID
    List<Document> findByOnboardingCaseCaseId(String caseId);

    // Find documents by file type
    List<Document> findByType(String type);

    // Custom query to find documents by case status
    @Query("SELECT d FROM Document d JOIN d.onboardingCase c WHERE c.status = :status")
    List<Document> findDocumentsByCaseStatus(@Param("status") String status);
    
    // Count documents per case
    @Query("SELECT COUNT(d) FROM Document d WHERE d.onboardingCase.caseId = :caseId")
    Long countDocumentsByCase(@Param("caseId") String caseId);
}
