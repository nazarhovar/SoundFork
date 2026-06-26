package com.SoundFork.SoundFork.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@Slf4j
public class CoverController {

    private static final Map<String, MediaType> EXTENSION_MEDIA_TYPES = Map.of(
            "png", MediaType.IMAGE_PNG,
            "gif", MediaType.IMAGE_GIF,
            "webp", MediaType.valueOf("image/webp"),
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG
    );

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @GetMapping("/covers/{projectId}/{filename}")
    public ResponseEntity<Resource> getCover(
            @PathVariable String projectId,
            @PathVariable String filename
    ) {
        if (!projectId.matches("\\d+")) {
            return ResponseEntity.badRequest().build();
        }
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path coverPath = Paths.get(uploadDir, "covers", projectId, filename).normalize();
        Path expectedParent = Paths.get(uploadDir, "covers", projectId).normalize();
        if (!coverPath.startsWith(expectedParent)) {
            return ResponseEntity.badRequest().build();
        }
        Resource resource = new FileSystemResource(coverPath);
        if (!resource.exists()) {
            log.warn("Cover not found: {}/{}/{}", uploadDir, projectId, filename);
            return ResponseEntity.notFound().build();
        }
        MediaType mt = resolveMediaType(filename);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(7, java.util.concurrent.TimeUnit.DAYS))
                .contentType(mt).body(resource);
    }

    private static MediaType resolveMediaType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return MediaType.IMAGE_JPEG;
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return EXTENSION_MEDIA_TYPES.getOrDefault(ext, MediaType.IMAGE_JPEG);
    }
}
