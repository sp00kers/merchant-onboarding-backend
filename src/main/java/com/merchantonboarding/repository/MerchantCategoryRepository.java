package com.merchantonboarding.repository;

import com.merchantonboarding.model.MerchantCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MerchantCategoryRepository extends JpaRepository<MerchantCategory, String> {
    Optional<MerchantCategory> findByCode(String code);
    List<MerchantCategory> findByStatus(String status);
    List<MerchantCategory> findByRiskLevel(String riskLevel);

    @Query("SELECT mc FROM MerchantCategory mc WHERE mc.code LIKE %:keyword% OR mc.name LIKE %:keyword% OR mc.description LIKE %:keyword%")
    List<MerchantCategory> searchMerchantCategories(@Param("keyword") String keyword);
}

