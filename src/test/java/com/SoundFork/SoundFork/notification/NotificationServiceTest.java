package com.SoundFork.SoundFork.notification;

import com.SoundFork.SoundFork.notification.dto.NotificationPageResponse;
import com.SoundFork.SoundFork.notification.entity.Notification;
import com.SoundFork.SoundFork.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("getUnreadCount returns count from notification-service")
    void getUnreadCount_returnsCount() {
        Map<String, Long> body = Map.of("count", 5L);
        when(restTemplate.exchange(
                contains("/notifications/unread/count"),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class))
        ).thenReturn(ResponseEntity.ok(body));

        long count = notificationService.getUnreadCount(1L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("getUnreadCount returns 0 on error")
    void getUnreadCount_onError_returnsZero() {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(),
                any(ParameterizedTypeReference.class))
        ).thenThrow(new RuntimeException("Connection refused"));

        long count = notificationService.getUnreadCount(1L);

        assertEquals(0L, count);
    }

    @Test
    @DisplayName("getNotificationsForUser returns page of notifications")
    void getNotifications_returnsPage() {
        NotificationPageResponse response = new NotificationPageResponse();
        Notification n = Notification.builder().id(1L).userId(1L).message("test").build();
        response.setContent(List.of(n));
        NotificationPageResponse.PageInfo pageInfo = new NotificationPageResponse.PageInfo();
        pageInfo.setNumber(0);
        pageInfo.setSize(20);
        pageInfo.setTotalElements(1);
        pageInfo.setTotalPages(1);
        response.setPage(pageInfo);

        when(restTemplate.exchange(
                contains("/notifications?page=0&size=20"),
                eq(HttpMethod.GET),
                any(),
                eq(NotificationPageResponse.class))
        ).thenReturn(ResponseEntity.ok(response));

        Page<Notification> result = notificationService.getNotificationsForUser(1L, PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test", result.getContent().get(0).getMessage());
    }

    @Test
    @DisplayName("getNotificationsForUser returns empty page on error")
    void getNotifications_onError_returnsEmpty() {
        when(restTemplate.exchange(
                anyString(),
                any(HttpMethod.class),
                any(),
                eq(NotificationPageResponse.class))
        ).thenThrow(new RuntimeException("Connection refused"));

        Page<Notification> result = notificationService.getNotificationsForUser(1L, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }
}
