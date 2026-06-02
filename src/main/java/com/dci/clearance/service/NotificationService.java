package com.dci.clearance.service;

import com.dci.clearance.dto.NotificationRequest;
import com.dci.clearance.dto.NotificationResponse;
import com.dci.clearance.entity.Notification;
import com.dci.clearance.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public NotificationResponse getById(Long id) {
        return notificationRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public NotificationResponse create(NotificationRequest request, String username, String userrole) {
        Notification notification = Notification.builder()
                .notifId(request.getNotifId())
                .notifDetails(request.getNotifDetails())
                .userstamp(username)
                .userrole(userrole)
                .build();

        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse update(Long id, NotificationRequest request, String username, String userrole) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setNotifId(request.getNotifId());
        notification.setNotifDetails(request.getNotifDetails());
        notification.setUserstamp(username);
        notification.setUserrole(userrole);

        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notifId(notification.getNotifId())
                .notifDetails(notification.getNotifDetails())
                .userstamp(notification.getUserstamp())
                .userrole(notification.getUserrole())
                .timestamp(notification.getTimestamp())
                .build();
    }
}
