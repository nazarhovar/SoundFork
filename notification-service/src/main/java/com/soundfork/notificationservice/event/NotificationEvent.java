package com.soundfork.notificationservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long userId;
    private String message;
    private String type;
    private Long relatedProjectId;
    private String relatedProjectTitle;
}
