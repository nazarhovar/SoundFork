package com.SoundFork.SoundFork.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;
    private String title;
    private String description;
    private String genre;
    private String coverArtPath;
    private AuthorInfo author;
    private Long sourceProjectId;
    private String sourceProjectTitle;
    private LocalDateTime forkedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long latestTrackId;
    private String latestTrackTitle;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private Long id;
        private String username;
    }
}
