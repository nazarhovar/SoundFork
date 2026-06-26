package com.soundfork.notificationservice.controller;

import com.soundfork.notificationservice.entity.Notification;
import com.soundfork.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications")
    public Page<Notification> getNotifications(
            @RequestHeader("X-Authenticated-User") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationService.getNotificationsForUser(userId, pageable);
    }

    @GetMapping("/notifications/unread/count")
    public Map<String, Long> getUnreadCount(
            @RequestHeader("X-Authenticated-User") Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return Map.of("count", count);
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id,
                           @RequestHeader("X-Authenticated-User") Long userId) {
        notificationService.markAsRead(id, userId);
    }

    @DeleteMapping("/notifications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestHeader("X-Authenticated-User") Long userId) {
        notificationService.deleteNotification(id, userId);
    }

    @DeleteMapping("/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll(@RequestHeader("X-Authenticated-User") Long userId) {
        notificationService.clearAllForUser(userId);
    }
}
