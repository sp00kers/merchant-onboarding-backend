package com.merchantonboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.AnalyticsDTO;
import com.merchantonboarding.service.AnalyticsService;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    /**
     * Get comprehensive dashboard analytics
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_VIEW', 'REPORTS', 'CASE_VIEW', 'CASE_CREATION', 'CASE_MANAGEMENT', 'ALL_MODULES')")
    public ResponseEntity<AnalyticsDTO> getDashboardAnalytics() {
        AnalyticsDTO analytics = analyticsService.getDashboardAnalytics();
        return ResponseEntity.ok(analytics);
    }

    /**
     * Get analytics for a specific date range
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyAuthority('DASHBOARD_VIEW', 'REPORTS', 'CASE_VIEW', 'CASE_CREATION', 'CASE_MANAGEMENT', 'ALL_MODULES')")
    public ResponseEntity<AnalyticsDTO> getAnalyticsByRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        AnalyticsDTO analytics = analyticsService.getAnalyticsByDateRange(startDate, endDate);
        return ResponseEntity.ok(analytics);
    }
}
