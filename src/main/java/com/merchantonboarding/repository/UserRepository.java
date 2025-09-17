package com.merchantonboarding.repository;

import com.merchantonboarding.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Derived query methods
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByEnabledTrue();
    
    // Custom JPQL query for assignment requirements
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findUsersByRole(@Param("roleName") String roleName);
    
    // Native SQL query for assignment requirements
    @Query(value = "SELECT * FROM users u WHERE u.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)", 
           nativeQuery = true)
    List<User> findRecentUsers();
}
