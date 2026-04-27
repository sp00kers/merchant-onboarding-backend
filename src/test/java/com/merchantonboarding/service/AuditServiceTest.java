package com.merchantonboarding.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merchantonboarding.dto.AuditLogDTO;
import com.merchantonboarding.model.AuditLog;
import com.merchantonboarding.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    private AuditLog testLog;

    @BeforeEach
    void setUp() {
        testLog = new AuditLog();
        testLog.setId(1L);
        testLog.setAction("CREATE_CASE");
        testLog.setEntityType("Case");
        testLog.setEntityId("MOP-2026-001");
        testLog.setUserId("USR001");
        testLog.setUserEmail("john.doe@bank.com");
        testLog.setIpAddress("192.168.1.1");
        testLog.setTimestamp(LocalDateTime.now());
        testLog.setStatus("SUCCESS");
        testLog.setDetails("Case created");
    }

    // ─── logAction() ──────────────────────────────────────

    // Test: logging an audit action saves the record with correct action type, entity type, and entity ID
    @Test
    void logAction_Success() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog log = inv.getArgument(0);
            log.setId(1L);
            return log;
        });

        AuditLog result = auditService.logAction("CREATE_CASE", "Case", "MOP-2026-001",
                "USR001", "john.doe@bank.com", "192.168.1.1",
                null, null, "SUCCESS", "Case created");

        assertNotNull(result);
        assertEquals("CREATE_CASE", result.getAction());
        assertEquals("Case", result.getEntityType());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    // Test: logging an action with null optional fields (entityId, userId, IP, etc.) still succeeds without errors
    @Test
    void logAction_NullValues() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        AuditLog result = auditService.logAction("LOGIN_SUCCESS", "User", null,
                null, "user@bank.com", null,
                null, null, "SUCCESS", null);

        assertNotNull(result);
        assertNull(result.getOldValue());
        assertNull(result.getNewValue());
    }

    // Test: when old/new value objects are provided, they are serialized to JSON strings for change tracking
    @Test
    void logAction_SerializesObjectToJson() throws JsonProcessingException {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":\"active\"}");
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        Object testObj = new Object() { public String status = "active"; };
        AuditLog result = auditService.logAction("UPDATE_USER", "User", "USR001",
                "USR002", "admin@bank.com", "192.168.1.1",
                testObj, testObj, "SUCCESS", "Updated user");

        assertNotNull(result);
        assertEquals("{\"status\":\"active\"}", result.getOldValue());
    }

    // ─── getAuditLogs() ──────────────────────────────────
    // Test: retrieving audit logs returns a paginated result sorted by newest first    @Test
    void getAuditLogs_Paginated() {
        Page<AuditLog> page = new PageImpl<>(List.of(testLog));
        when(auditLogRepository.findAllByOrderByTimestampDesc(any(Pageable.class))).thenReturn(page);

        Page<AuditLogDTO> result = auditService.getAuditLogs(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals("CREATE_CASE", result.getContent().get(0).getAction());
    }

    // Test: retrieving audit logs with all filters (entity type, action, userId, date range) returns matching results
    @Test
    void getAuditLogsWithFilters_AllFilters() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();
        Page<AuditLog> page = new PageImpl<>(List.of(testLog));

        when(auditLogRepository.findWithFilters(
                eq("Case"), eq("CREATE_CASE"), eq("USR001"),
                eq(start), eq(end), any(Pageable.class))).thenReturn(page);

        Page<AuditLogDTO> result = auditService.getAuditLogsWithFilters(
                "Case", "CREATE_CASE", "USR001", start, end, 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    // Test: retrieving all audit logs for a specific entity (e.g. Case MOP-2026-001) returns its full history
    @Test
    void getAuditLogsForEntity_ReturnsList() {
        when(auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("Case", "MOP-2026-001"))
                .thenReturn(List.of(testLog));

        List<AuditLogDTO> result = auditService.getAuditLogsForEntity("Case", "MOP-2026-001");

        assertEquals(1, result.size());
        assertEquals("MOP-2026-001", result.get(0).getEntityId());
    }

    // Test: retrieving all audit logs for a specific user returns paginated results of their actions
    @Test
    void getAuditLogsForUser_Paginated() {
        Page<AuditLog> page = new PageImpl<>(List.of(testLog));
        when(auditLogRepository.findByUserIdOrderByTimestampDesc(eq("USR001"), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditLogDTO> result = auditService.getAuditLogsForUser("USR001", 0, 10);

        assertEquals(1, result.getTotalElements());
    }

    // ─── Distinct queries ──────────────────────────────────

    // Test: getDistinctActions returns all unique action types (e.g. CREATE_CASE, UPDATE_CASE) for filter dropdowns in the UI
    @Test
    void getDistinctActions_ReturnsList() {
        when(auditLogRepository.findDistinctActions())
                .thenReturn(List.of("CREATE_CASE", "UPDATE_CASE", "DELETE_CASE"));

        List<String> result = auditService.getDistinctActions();

        assertEquals(3, result.size());
        assertTrue(result.contains("CREATE_CASE"));
    }

    // Test: getDistinctEntityTypes returns all unique entity types (e.g. Case, User, Role) for filter dropdowns in the UI
    @Test
    void getDistinctEntityTypes_ReturnsList() {
        when(auditLogRepository.findDistinctEntityTypes())
                .thenReturn(List.of("Case", "User", "Role"));

        List<String> result = auditService.getDistinctEntityTypes();

        assertEquals(3, result.size());
        assertTrue(result.contains("Case"));
    }

    // ─── Count queries ──────────────────────────────────
    // Test: counting total audit events since a given timestamp (used for dashboard activity statistics)    @Test
    void getAuditCountSince_ReturnsCount() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(auditLogRepository.countByTimestampAfter(since)).thenReturn(42L);

        long result = auditService.getAuditCountSince(since);

        assertEquals(42L, result);
    }

    // Test: counting login failures since a given timestamp (used for security monitoring on the dashboard)
    @Test
    void getLoginFailureCountSince_ReturnsCount() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(auditLogRepository.countByActionAndTimestampAfter("LOGIN_FAILED", since)).thenReturn(5L);

        long result = auditService.getLoginFailureCountSince(since);

        assertEquals(5L, result);
    }
}
