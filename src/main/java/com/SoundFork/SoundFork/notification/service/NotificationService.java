package com.SoundFork.SoundFork.notification.service;

import com.SoundFork.SoundFork.notification.dto.NotificationPageResponse;
import com.SoundFork.SoundFork.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RestTemplate restTemplate;

    @Value("${notification.service.url}")
    private String notificationServiceUrl;

    public Page<Notification> getNotificationsForUser(Long userId, Pageable pageable) {
        String url = String.format("%s/notifications?page=%d&size=%d",
                notificationServiceUrl, pageable.getPageNumber(), pageable.getPageSize());
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User", String.valueOf(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            NotificationPageResponse response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, NotificationPageResponse.class).getBody();
            if (response == null || response.getContent() == null) {
                return Page.empty();
            }
            NotificationPageResponse.PageInfo pageInfo = response.getPage();
            return new PageImpl<>(
                    response.getContent(),
                    PageRequest.of(pageInfo.getNumber(), pageInfo.getSize()),
                    pageInfo.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Failed to fetch notifications: {} {}", e.getMessage(), e.getClass().getSimpleName());
            return Page.empty();
        }
    }

    public long getUnreadCount(Long userId) {
        String url = notificationServiceUrl + "/notifications/unread/count";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User", String.valueOf(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            Map<String, Long> response = restTemplate.exchange(url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Long>>() {}).getBody();
            return response != null ? response.getOrDefault("count", 0L) : 0L;
        } catch (Exception e) {
            log.error("Failed to fetch unread count: {} {}", e.getMessage(), e.getClass().getSimpleName());
            return 0L;
        }
    }

    public void markAsRead(Long notificationId, Long userId) {
        String url = notificationServiceUrl + "/notifications/" + notificationId + "/read";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User", String.valueOf(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }

    public void deleteNotification(Long notificationId, Long userId) {
        String url = notificationServiceUrl + "/notifications/" + notificationId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User", String.valueOf(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public void clearAllForUser(Long userId) {
        String url = notificationServiceUrl + "/notifications";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Authenticated-User", String.valueOf(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
