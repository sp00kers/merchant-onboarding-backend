package com.merchantonboarding.repository;

import com.merchantonboarding.model.OnboardingCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseRepository extends JpaRepository<OnboardingCase, String> {

    // Derived query methods for assignment requirements
    List<OnboardingCase> findByStatus(String status);
    List<OnboardingCase> findByAssignedTo(String assignedTo);
    Page<OnboardingCase> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    // JPQL queries for assignment requirements
    @Query("SELECT c FROM OnboardingCase c WHERE c.businessName LIKE %:keyword% OR c.businessType LIKE %:keyword% OR c.merchantCategory LIKE %:keyword%")
    List<OnboardingCase> searchCases(@Param("keyword") String keyword);
    
    @Query("SELECT c FROM OnboardingCase c WHERE c.createdDate BETWEEN :startDate AND :endDate")
    List<OnboardingCase> findCasesByDateRange(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    // Native SQL query for complex reporting
    @Query(value = "SELECT status, COUNT(*) as count FROM onboarding_cases " +
                   "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                   "GROUP BY status", nativeQuery = true)
    List<Object[]> getCaseStatusStatistics();

    // Count by status
    long countByStatus(String status);
}
