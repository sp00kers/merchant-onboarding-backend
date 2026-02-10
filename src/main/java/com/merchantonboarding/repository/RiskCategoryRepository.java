package com.merchantonboarding.repository;

import com.merchantonboarding.model.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RiskCategoryRepository extends JpaRepository<RiskCategory, String> {
    Optional<RiskCategory> findByLevel(Integer level);
    Optional<RiskCategory> findByName(String name);
    List<RiskCategory> findAllByOrderByLevelAsc();
}

