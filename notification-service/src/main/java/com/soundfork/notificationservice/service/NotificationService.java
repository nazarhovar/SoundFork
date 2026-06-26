package com.soundfork.notificationservice.service;

import com.soundfork.notificationservice.entity.Notification;
import com.soundfork.notificationservice.exception.NotificationNotFoundException;
import com.soundfork.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(Long userId, String message, String type,
                                    Long relatedProjectId, String relatedProjectTitle) {
        Notification n = Notification.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .relatedProjectId(relatedProjectId)
                .relatedProjectTitle(relatedProjectTitle)
                .build();
        notificationRepository.save(n);
        log.info("Saved notification for userId={}, type={}", userId, type);
    }

    public Page<Notification> getNotificationsForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new IllegalStateException("Notification does not belong to this user");
        }
        n.setRead(true);
    }

    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!n.getUserId().equals(userId)) {
            throw new IllegalStateException("Notification does not belong to this user");
        }
        notificationRepository.delete(n);
    }

    @Transactional
    public void clearAllForUser(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
