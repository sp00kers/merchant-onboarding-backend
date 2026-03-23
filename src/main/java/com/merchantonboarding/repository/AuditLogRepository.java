package com.merchantonboarding.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.merchantonboarding.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    @Query("SELECT DISTINCT a.action FROM AuditLog a ORDER BY a.action")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a WHERE a.entityType IS NOT NULL ORDER BY a.entityType")
    List<String> findDistinctEntityTypes();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:entityType IS NULL OR a.entityType = :entityType) AND " +
           "(:action IS NULL OR a.action = :action) AND " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findWithFilters(
        @Param("entityType") String entityType,
        @Param("action") String action,
        @Param("userId") String userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    long countByTimestampAfter(LocalDateTime timestamp);

    long countByActionAndTimestampAfter(String action, LocalDateTime timestamp);
}
