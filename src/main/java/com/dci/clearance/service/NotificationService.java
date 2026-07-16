package com.dci.clearance.service;

import com.dci.clearance.entity.User;
import com.dci.clearance.entity.UserNotification;
import com.dci.clearance.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void notifyUser(User user, String title, String message, String type, Long referenceId) {
        UserNotification notification = UserNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", user.getUsername(), title);
    }

    @Transactional
    public void notifyUserWithVerification(User user, String title, String message, String type, Long referenceId,
                                           String plateNo, String verifierName) {
        notifyUser(user, title, message, type, referenceId);

        if (user.getEmail() != null && !user.getEmail().isBlank()
                && ("HPG_VERIFIED".equals(type) || "DCI_VALIDATED".equals(type))) {
            String firstName = user.getFirstName() != null ? user.getFirstName() : user.getUsername();
            emailService.sendVerificationNotification(
                    user.getEmail(), firstName, plateNo, verifierName, type
            );
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotifications(Long userId) {
        List<UserNotification> notifications = notificationRepository.findByUserIdOrderByDateCreatedDesc(userId);
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        List<Map<String, Object>> items = notifications.stream().map(n -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("title", n.getTitle());
            map.put("message", n.getMessage());
            map.put("type", n.getType());
            map.put("referenceId", n.getReferenceId());
            map.put("isRead", n.getIsRead());
            map.put("dateCreated", n.getDateCreated() != null ? n.getDateCreated().toString() : null);
            return map;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", items);
        response.put("unreadCount", unreadCount);
        return response;
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.markAsReadById(id, userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
