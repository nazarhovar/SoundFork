package com.SoundFork.SoundFork.notification.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${notification.topic}")
    private String topic;

    public void publishNotification(Long userId, String message, String type,
                                     Long relatedProjectId, String relatedProjectTitle) {
        NotificationEvent event = NotificationEvent.builder()
                .userId(userId)
                .message(message)
                .type(type)
                .relatedProjectId(relatedProjectId)
                .relatedProjectTitle(relatedProjectTitle)
                .build();

        kafkaTemplate.send(topic, event);
        log.info("Published notification event: userId={}, type={}", userId, type);
    }
}
