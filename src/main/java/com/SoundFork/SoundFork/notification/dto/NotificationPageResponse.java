package com.SoundFork.SoundFork.notification.dto;

import com.SoundFork.SoundFork.notification.entity.Notification;
import lombok.Data;

import java.util.List;

@Data
public class NotificationPageResponse {
    private List<Notification> content;
    private PageInfo page;

    @Data
    public static class PageInfo {
        private int size;
        private long totalElements;
        private int totalPages;
        private int number;
    }
}
