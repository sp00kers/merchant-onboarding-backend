package com.merchantonboarding.repository;

import com.merchantonboarding.model.BusinessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessTypeRepository extends JpaRepository<BusinessType, String> {
    Optional<BusinessType> findByCode(String code);
    List<BusinessType> findByStatus(String status);

    @Query("SELECT bt FROM BusinessType bt WHERE bt.code LIKE %:keyword% OR bt.name LIKE %:keyword% OR bt.description LIKE %:keyword%")
    List<BusinessType> searchBusinessTypes(@Param("keyword") String keyword);
}

