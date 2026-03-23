package com.merchantonboarding.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merchantonboarding.dto.AuditLogDTO;
import com.merchantonboarding.model.AuditLog;
import com.merchantonboarding.repository.AuditLogRepository;

@Service
@Transactional
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    public AuditLog logAction(String action, String entityType, String entityId,
                              String userId, String userEmail, String ipAddress,
                              Object oldValue, Object newValue, String status, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setUserId(userId);
        log.setUserEmail(userEmail);
        log.setIpAddress(ipAddress);
        log.setOldValue(serializeToJson(oldValue));
        log.setNewValue(serializeToJson(newValue));
        log.setTimestamp(LocalDateTime.now());
        log.setStatus(status);
        log.setDetails(details);

        return auditLogRepository.save(log);
    }

    public AuditLog logAction(String action, String entityType, String entityId,
                              String userId, String userEmail, String status, String details) {
        return logAction(action, entityType, entityId, userId, userEmail, null, null, null, status, details);
    }

    public Page<AuditLogDTO> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
            .map(this::convertToDTO);
    }

    public Page<AuditLogDTO> getAuditLogsWithFilters(String entityType, String action,
                                                      String userId, LocalDateTime startDate,
                                                      LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findWithFilters(entityType, action, userId, startDate, endDate, pageable)
            .map(this::convertToDTO);
    }

    public List<AuditLogDTO> getAuditLogsForEntity(String entityType, String entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId)
            .stream()
            .map(this::convertToDTO)
            .toList();
    }

    public Page<AuditLogDTO> getAuditLogsForUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
            .map(this::convertToDTO);
    }

    public List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }

    public List<String> getDistinctEntityTypes() {
        return auditLogRepository.findDistinctEntityTypes();
    }

    public long getAuditCountSince(LocalDateTime since) {
        return auditLogRepository.countByTimestampAfter(since);
    }

    public long getLoginFailureCountSince(LocalDateTime since) {
        return auditLogRepository.countByActionAndTimestampAfter("LOGIN_FAILED", since);
    }

    private String serializeToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    private AuditLogDTO convertToDTO(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setAction(log.getAction());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setUserId(log.getUserId());
        dto.setUserEmail(log.getUserEmail());
        dto.setIpAddress(log.getIpAddress());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        dto.setTimestamp(log.getTimestamp());
        dto.setStatus(log.getStatus());
        dto.setDetails(log.getDetails());
        return dto;
    }
}
