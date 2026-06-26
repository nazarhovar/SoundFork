package com.SoundFork.SoundFork.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    private Long userId;
    private String message;
    private String type;
    private Long relatedProjectId;
    private String relatedProjectTitle;
}
