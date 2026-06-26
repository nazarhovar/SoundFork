package com.SoundFork.SoundFork.track.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackResponse {

    private Long id;
    private String title;
    private Long projectId;
    private String fileName;
    private Long fileSize;
    private String fileFormat;
    private String downloadUrl;
    private Integer bpm;
    private String musicalKey;
    private LocalDateTime createdAt;
}
