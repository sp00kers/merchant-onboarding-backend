package com.merchantonboarding.repository;

import com.merchantonboarding.model.CaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CaseHistoryRepository extends JpaRepository<CaseHistory, Long> {
    List<CaseHistory> findByOnboardingCaseCaseIdOrderByTimeDesc(String caseId);
}

