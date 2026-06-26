package com.SoundFork.SoundFork.version.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {

    private Long id;
    private Long projectId;
    private Long parentVersionId;
    private Integer versionNumber;
    private String commitMessage;

    private AuthorInfo author;

    private LocalDateTime createdAt;

    private boolean hasApprovedMergeRequest;
    private boolean hasMergeRequest;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorInfo {
        private Long id;
        private String username;
    }
}
