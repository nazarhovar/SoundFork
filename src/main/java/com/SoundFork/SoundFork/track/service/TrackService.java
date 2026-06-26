package com.SoundFork.SoundFork.track.service;

import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.project.repository.ProjectRepository;
import com.SoundFork.SoundFork.project.service.ProjectNotFoundException;
import com.SoundFork.SoundFork.track.dto.TrackResponse;
import com.SoundFork.SoundFork.track.entity.Track;
import com.SoundFork.SoundFork.track.repository.TrackRepository;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class TrackService {
    private static final Set<String> ALLOWED_FORMATS = Set.of("mp3", "wav", "flac", "ogg", "aac", "wma", "m4a");

    private final TrackRepository trackRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir, "tracks"));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Transactional
    public TrackResponse upload(Long projectId, MultipartFile file, String title, Integer bpm, String musicalKey, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("Only the project author can upload tracks directly. Fork the project first.");
        }

        validateFile(file);
        String fileFormat = resolveFileFormat(file);
        String resolvedTitle = resolveTitle(title, file.getOriginalFilename());

        log.info("Uploading track: projectId={}, title={}, format={}, size={}",
                projectId, resolvedTitle, fileFormat, file.getSize());

        String filePath = saveFileToDisk(projectId, file, fileFormat);
        Track savedTrack = createTrack(project, resolvedTitle, filePath, file.getSize(), fileFormat, bpm, musicalKey);

        return buildTrackResponse(savedTrack);
    }

    @Transactional(readOnly = true)
    public Resource getFileAsResource(Long id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new TrackNotFoundException(id));

        Path filePath = Paths.get(track.getFilePath());
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            throw new RuntimeException("Track file not found on disk: " + track.getFilePath());
        }

        return resource;
    }

    @Transactional(readOnly = true)
    public List<TrackResponse> getTracksByProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        return trackRepository.findByProjectIdOrderByIdAsc(projectId)
                .stream()
                .map(this::buildTrackResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrackResponse getById(Long id) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new TrackNotFoundException(id));

        return buildTrackResponse(track);
    }

    @Transactional
    public void delete(Long id, String username) {
        Track track = trackRepository.findById(id)
                .orElseThrow(() -> new TrackNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        Project project = track.getProject();
        if (!project.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the project author can delete tracks");
        }
        try {
            Files.deleteIfExists(Paths.get(track.getFilePath()));
        } catch (IOException e) {
            log.warn("Could not delete file from disk: {}", track.getFilePath(), e);
        }
        trackRepository.delete(track);

        log.info("Deleted track: id={}, title={}", id, track.getTitle());
    }

    private TrackResponse buildTrackResponse(Track track) {
        String fileName = Paths.get(track.getFilePath()).getFileName().toString();

        return TrackResponse.builder()
                .id(track.getId())
                .title(track.getTitle())
                .projectId(track.getProject().getId())
                .fileName(fileName)
                .fileSize(track.getFileSize())
                .fileFormat(track.getFileFormat())
                .downloadUrl("/tracks/" + track.getId() + "/download")
                .bpm(track.getBpm())
                .musicalKey(track.getMusicalKey())
                .createdAt(track.getCreatedAt())
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
    }

    private String resolveFileFormat(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String format = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!ALLOWED_FORMATS.contains(format)) {
                throw new IllegalArgumentException(
                        "Unsupported file format: " + format +
                        ". Allowed formats: " + String.join(", ", ALLOWED_FORMATS)
                );
            }
            return format;
        }
        throw new IllegalArgumentException("Could not determine file format");
    }

    private String resolveTitle(String title, String originalFilename) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(0, originalFilename.lastIndexOf("."));
        }
        return originalFilename != null ? originalFilename : "untitled";
    }

    private String saveFileToDisk(Long projectId, MultipartFile file, String fileFormat) {
        try {
            String uniqueFileName = UUID.randomUUID() + "." + fileFormat;
            Path projectDir = Paths.get(uploadDir, "tracks", projectId.toString());
            Files.createDirectories(projectDir);
            Path targetPath = projectDir.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Error saving file: " + e.getMessage(), e);
        }
    }

    @Transactional
    protected Track createTrack(Project project, String title, String filePath,
                                 long fileSize, String fileFormat, Integer bpm, String musicalKey) {
        Track track = Track.builder()
                .title(title)
                .project(project)
                .filePath(filePath)
                .fileSize(fileSize)
                .fileFormat(fileFormat)
                .bpm(bpm)
                .musicalKey(musicalKey)
                .build();
        return trackRepository.save(track);
    }
}
