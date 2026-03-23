package com.merchantonboarding.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.merchantonboarding.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    long countByUserIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.isRead = false")
    int markAllAsReadForUser(@Param("userId") String userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id")
    int markAsRead(@Param("id") Long id);

    List<Notification> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.category = :category ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndCategory(@Param("userId") String userId, @Param("category") String category);
}
