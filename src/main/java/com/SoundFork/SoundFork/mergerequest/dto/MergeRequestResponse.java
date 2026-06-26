package com.SoundFork.SoundFork.mergerequest.dto;

import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeRequestResponse {

    private Long id;
    private Long sourceVersionId;
    private Integer sourceVersionNumber;
    private Long targetProjectId;
    private String targetProjectTitle;
    private Long authorId;
    private String authorUsername;
    private MergeRequestStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
