package com.soundfork.notificationservice.event;

import com.soundfork.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${notification.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: userId={}, message={}, type={}",
                event.getUserId(), event.getMessage(), event.getType());
        notificationService.createNotification(
                event.getUserId(),
                event.getMessage(),
                event.getType(),
                event.getRelatedProjectId(),
                event.getRelatedProjectTitle()
        );
    }
}
