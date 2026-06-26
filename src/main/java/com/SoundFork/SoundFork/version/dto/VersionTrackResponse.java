package com.SoundFork.SoundFork.version.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionTrackResponse {

    private Long id;
    private Long trackId;
    private String trackTitle;
    private String fileFormat;
    private Long fileSize;
    private String downloadUrl;
    private Integer bpm;
    private String musicalKey;
}
