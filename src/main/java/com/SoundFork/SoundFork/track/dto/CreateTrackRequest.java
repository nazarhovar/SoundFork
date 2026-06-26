package com.SoundFork.SoundFork.track.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class CreateTrackRequest {
    private String title;
    private MultipartFile file;
    private Integer trackOrder;
}
