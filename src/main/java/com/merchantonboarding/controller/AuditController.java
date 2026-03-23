package com.merchantonboarding.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.dto.AuditLogDTO;
import com.merchantonboarding.service.AuditService;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<Page<AuditLogDTO>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        Page<AuditLogDTO> logs;

        if (entityType != null || action != null || userId != null || startDate != null || endDate != null) {
            logs = auditService.getAuditLogsWithFilters(entityType, action, userId, startDate, endDate, page, size);
        } else {
            logs = auditService.getAuditLogs(page, size);
        }

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<List<AuditLogDTO>> getLogsForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {

        List<AuditLogDTO> logs = auditService.getAuditLogsForEntity(entityType, entityId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<Page<AuditLogDTO>> getLogsForUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AuditLogDTO> logs = auditService.getAuditLogsForUser(userId, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/actions")
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<List<String>> getDistinctActions() {
        return ResponseEntity.ok(auditService.getDistinctActions());
    }

    @GetMapping("/entity-types")
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<List<String>> getDistinctEntityTypes() {
        return ResponseEntity.ok(auditService.getDistinctEntityTypes());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('ALL_MODULES', 'AUDIT_VIEW')")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        stats.put("actionsLast24Hours", auditService.getAuditCountSince(last24Hours));
        stats.put("actionsLast7Days", auditService.getAuditCountSince(last7Days));
        stats.put("loginFailuresLast24Hours", auditService.getLoginFailureCountSince(last24Hours));

        return ResponseEntity.ok(stats);
    }
}
