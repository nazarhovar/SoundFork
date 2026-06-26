package com.SoundFork.SoundFork.version.controller;

import com.SoundFork.SoundFork.version.dto.CreateVersionRequest;
import com.SoundFork.SoundFork.version.dto.VersionResponse;
import com.SoundFork.SoundFork.version.dto.VersionTrackResponse;
import com.SoundFork.SoundFork.version.service.VersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class VersionController {

    private final VersionService versionService;

    @PostMapping("/projects/{projectId}/versions/with-track")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse createWithTrack(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("commitMessage") String commitMessage,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "bpm", required = false) Integer bpm,
            @RequestParam(value = "musicalKey", required = false) String musicalKey,
            Authentication authentication
    ) {
        String username = authentication.getName();
        if (commitMessage == null || commitMessage.isBlank()) {
            throw new IllegalArgumentException("Commit message is required");
        }
        log.info("Create version with track: projectId={}, user={}, message={}", projectId, username, commitMessage);
        CreateVersionRequest request = new CreateVersionRequest();
        request.setCommitMessage(commitMessage);
        return versionService.createWithTrack(projectId, request, username, file, title, bpm, musicalKey);
    }

    @PostMapping("/projects/{projectId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse create(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateVersionRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("Create version: projectId={}, user={}", projectId, username);
        return versionService.create(projectId, request, username);
    }

    @GetMapping("/projects/{projectId}/versions")
    public List<VersionResponse> getHistory(@PathVariable Long projectId) {
        return versionService.getVersionHistory(projectId);
    }

    @GetMapping("/versions/{id}")
    public VersionResponse getById(@PathVariable Long id) {
        return versionService.getById(id);
    }

    @GetMapping("/versions/{id}/tracks")
    public List<VersionTrackResponse> getTracks(@PathVariable Long id) {
        return versionService.getTracksInVersion(id);
    }

    @DeleteMapping("/versions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        versionService.delete(id, authentication.getName());
    }
}
