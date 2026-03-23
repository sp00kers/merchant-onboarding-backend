package com.merchantonboarding.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NotificationDTO {
    private Long id;
    private String userId;
    private String title;
    private String message;
    private String type;
    private String category;
    private String relatedEntityType;
    private String relatedEntityId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
