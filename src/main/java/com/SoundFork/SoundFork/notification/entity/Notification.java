package com.SoundFork.SoundFork.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    private Long id;
    private Long userId;
    private String message;
    private String type;
    private Long relatedProjectId;
    private String relatedProjectTitle;
    private boolean read;
    private LocalDateTime createdAt;
}
