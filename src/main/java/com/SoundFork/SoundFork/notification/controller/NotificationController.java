package com.SoundFork.SoundFork.notification.controller;

import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.notification.entity.Notification;
import com.SoundFork.SoundFork.notification.service.NotificationService;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping("/notifications")
    public Page<Notification> getNotifications(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getUserId(authentication);
        return notificationService.getNotificationsForUser(userId, pageable);
    }

    @GetMapping("/notifications/unread/count")
    public long getUnreadCount(Authentication authentication) {
        Long userId = getUserId(authentication);
        return notificationService.getUnreadCount(userId);
    }

    @PostMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        notificationService.markAsRead(id, userId);
    }

    @DeleteMapping("/notifications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserId(authentication);
        notificationService.deleteNotification(id, userId);
    }

    @DeleteMapping("/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll(Authentication authentication) {
        Long userId = getUserId(authentication);
        notificationService.clearAllForUser(userId);
    }

    private Long getUserId(Authentication auth) {
        String username = auth.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"))
                .getId();
    }
}
