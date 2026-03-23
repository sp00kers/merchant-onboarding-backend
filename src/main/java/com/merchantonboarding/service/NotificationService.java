package com.merchantonboarding.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.merchantonboarding.dto.NotificationDTO;
import com.merchantonboarding.model.Notification;
import com.merchantonboarding.model.User;
import com.merchantonboarding.repository.NotificationRepository;
import com.merchantonboarding.repository.UserRepository;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private EmailService emailService;

    public Notification createNotification(String userId, String title, String message,
                                           String type, String category,
                                           String relatedEntityType, String relatedEntityId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type != null ? type : "INFO");
        notification.setCategory(category);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);

        // Send real-time notification via WebSocket
        sendWebSocketNotification(userId, saved);

        return saved;
    }

    public void sendWebSocketNotification(String userId, Notification notification) {
        if (messagingTemplate != null) {
            try {
                NotificationDTO dto = convertToDTO(notification);
                messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", dto);
            } catch (Exception e) {
                // Log but don't fail if WebSocket is unavailable
                System.err.println("Failed to send WebSocket notification: " + e.getMessage());
            }
        }
    }

    public void notifyUser(String userId, String title, String message, String type,
                           String category, String relatedEntityType, String relatedEntityId,
                           boolean sendEmail) {
        // Create in-app notification
        Notification notification = createNotification(userId, title, message, type, category,
                relatedEntityType, relatedEntityId);

        // Send email if requested
        if (sendEmail && emailService != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            userOpt.ifPresent(user -> {
                if (user.getEmail() != null) {
                    emailService.sendNotificationEmail(user.getEmail(), title, message);
                }
            });
        }
    }

    // Notify multiple users (e.g., all admins)
    public void notifyUsers(List<String> userIds, String title, String message,
                            String type, String category,
                            String relatedEntityType, String relatedEntityId,
                            boolean sendEmail) {
        for (String userId : userIds) {
            notifyUser(userId, title, message, type, category, relatedEntityType, relatedEntityId, sendEmail);
        }
    }

    // Notify users by role
    public void notifyUsersByRole(String roleId, String title, String message,
                                   String type, String category,
                                   String relatedEntityType, String relatedEntityId,
                                   boolean sendEmail) {
        List<User> users = userRepository.findUsersByRole(roleId);
        List<String> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        notifyUsers(userIds, title, message, type, category, relatedEntityType, relatedEntityId, sendEmail);
    }

    public Page<NotificationDTO> getNotificationsForUser(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::convertToDTO);
    }

    public List<NotificationDTO> getRecentNotifications(String userId) {
        return notificationRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    public void deleteNotification(Long id) {
        notificationRepository.deleteById(id);
    }

    // === Case-specific notification methods ===

    public void notifyCaseCreated(String caseId, String businessName, String creatorId, String assignedToId) {
        String title = "New Case Created";
        String message = String.format("Case %s for '%s' has been created and requires review.", caseId, businessName);

        // Notify assigned reviewer
        if (assignedToId != null && !assignedToId.isEmpty()) {
            notifyUser(assignedToId, title, message, "INFO", "CASE_STATUS", "Case", caseId, true);
        }

        // Notify all admins
        notifyUsersByRole("admin", title, message, "INFO", "CASE_STATUS", "Case", caseId, false);
    }

    public void notifyCaseStatusChanged(String caseId, String businessName, String oldStatus,
                                         String newStatus, String changedByUserId, String caseOwnerId) {
        String title = "Case Status Updated";
        String message = String.format("Case %s ('%s') status changed from '%s' to '%s'.",
                caseId, businessName, oldStatus, newStatus);

        // Notify case owner/creator
        if (caseOwnerId != null && !caseOwnerId.equals(changedByUserId)) {
            notifyUser(caseOwnerId, title, message, "INFO", "CASE_STATUS", "Case", caseId, true);
        }
    }

    public void notifyCaseAssigned(String caseId, String businessName, String assignedToId, String assignedByUserId) {
        String title = "Case Assigned to You";
        String message = String.format("Case %s ('%s') has been assigned to you for review.", caseId, businessName);

        notifyUser(assignedToId, title, message, "INFO", "ASSIGNMENT", "Case", caseId, true);
    }

    public void notifyVerificationComplete(String caseId, String businessName, String verificationType,
                                            int confidenceScore, String reviewerId) {
        String title = "Verification Complete";
        String message = String.format("Verification (%s) for case %s ('%s') completed with confidence score: %d%%.",
                verificationType, caseId, businessName, confidenceScore);

        notifyUser(reviewerId, title, message, "SUCCESS", "VERIFICATION", "Case", caseId, true);
    }

    public void notifyDocumentUploaded(String caseId, String businessName, int documentCount, String reviewerId) {
        String title = "Documents Uploaded";
        String message = String.format("%d document(s) uploaded for case %s ('%s').", documentCount, caseId, businessName);

        if (reviewerId != null) {
            notifyUser(reviewerId, title, message, "INFO", "CASE_STATUS", "Case", caseId, false);
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setCategory(notification.getCategory());
        dto.setRelatedEntityType(notification.getRelatedEntityType());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setReadAt(notification.getReadAt());
        return dto;
    }
}
