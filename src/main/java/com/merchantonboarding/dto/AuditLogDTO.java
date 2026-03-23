package com.merchantonboarding.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private Long id;
    private String action;
    private String entityType;
    private String entityId;
    private String userId;
    private String userEmail;
    private String ipAddress;
    private String oldValue;
    private String newValue;
    private LocalDateTime timestamp;
    private String status;
    private String details;
}
