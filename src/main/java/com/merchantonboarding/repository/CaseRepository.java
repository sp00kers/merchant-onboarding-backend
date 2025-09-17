package com.merchantonboarding.repository;

import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.OnboardingCase.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<OnboardingCase, Long> {
    
    // Derived query methods for assignment requirements
    List<OnboardingCase> findByStatus(CaseStatus status);
    List<OnboardingCase> findByAssignedOfficerId(Long officerId);
    Page<OnboardingCase> findByStatusOrderByCreatedAtDesc(CaseStatus status, Pageable pageable);
    
    // JPQL queries for assignment requirements
    @Query("SELECT c FROM OnboardingCase c WHERE c.merchantName LIKE %:keyword% OR c.businessType LIKE %:keyword%")
    List<OnboardingCase> searchCases(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM OnboardingCase c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<OnboardingCase> findCasesByDateRange(@Param("startDate") LocalDateTime startDate, 
                                              @Param("endDate") LocalDateTime endDate);
    
    // Native SQL query for complex reporting
    @Query(value = "SELECT status, COUNT(*) as count FROM onboarding_cases " +
                   "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                   "GROUP BY status", nativeQuery = true)
    List<Object[]> getCaseStatusStatistics();
}
