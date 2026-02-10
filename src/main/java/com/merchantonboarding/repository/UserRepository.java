package com.merchantonboarding.repository;

import com.merchantonboarding.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Derived query methods
    Optional<User> findByEmail(String email);
    List<User> findByStatus(String status);
    List<User> findByRoleId(String roleId);

    // Custom JPQL query for assignment requirements
    @Query("SELECT u FROM User u WHERE u.role.id = :roleId")
    List<User> findUsersByRole(@Param("roleId") String roleId);

    // Native SQL query for assignment requirements
    @Query(value = "SELECT * FROM users u WHERE u.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", 
           nativeQuery = true)
    List<User> findRecentUsers();

    // Search users
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword% OR u.email LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);
}
