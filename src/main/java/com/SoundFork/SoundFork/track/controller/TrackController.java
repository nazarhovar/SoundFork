package com.SoundFork.SoundFork.track.controller;

import com.SoundFork.SoundFork.track.dto.TrackResponse;
import com.SoundFork.SoundFork.track.service.TrackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TrackController {

    private final TrackService trackService;
    @PostMapping("/projects/{projectId}/tracks")
    @ResponseStatus(HttpStatus.CREATED)
    public TrackResponse upload(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "bpm", required = false) Integer bpm,
            @RequestParam(value = "musicalKey", required = false) String musicalKey,
            Authentication authentication
    ) {
        log.info("Upload track: projectId={}, title={}, user={}", projectId, title, authentication.getName());
        return trackService.upload(projectId, file, title, bpm, musicalKey, authentication.getName());
    }

    @GetMapping("/projects/{projectId}/tracks")
    public List<TrackResponse> getTracksByProject(@PathVariable Long projectId) {
        return trackService.getTracksByProject(projectId);
    }

    @GetMapping("/tracks/{id}")
    public TrackResponse getById(@PathVariable Long id) {
        return trackService.getById(id);
    }

    @GetMapping("/tracks/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = trackService.getFileAsResource(id);

        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            contentType = MediaType.parseMediaType(
                    "audio/" + resource.getFilename().substring(
                            resource.getFilename().lastIndexOf(".") + 1
                    )
            ).toString();
        } catch (Exception e) {
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/tracks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        log.info("Delete track: id={}, user={}", id, authentication.getName());
        trackService.delete(id, authentication.getName());
    }
}
