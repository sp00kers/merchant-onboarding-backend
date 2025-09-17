package com.merchantonboarding.controller;

import com.merchantonboarding.service.CaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    @Autowired
    private CaseService caseService;

    /**
     * Get dashboard statistics for the main dashboard view
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Get case statistics
        Map<String, Long> caseStats = caseService.getCaseStatistics();
        stats.put("caseStatistics", caseStats);
        
        // Calculate total cases
        long totalCases = caseStats.values().stream().mapToLong(Long::longValue).sum();
        stats.put("totalCases", totalCases);
        
        // Get recent activity (you can implement this based on your needs)
        stats.put("recentActivity", "Dashboard loaded successfully");
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get user-specific dashboard data
     */
    @GetMapping("/user-stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserDashboardStats(@PathVariable Long userId) {
        Map<String, Object> userStats = new HashMap<>();
        
        // Get cases assigned to user
        userStats.put("userCases", caseService.getCasesByOfficer(userId));
        userStats.put("message", "User dashboard data loaded");
        
        return ResponseEntity.ok(userStats);
    }
}
