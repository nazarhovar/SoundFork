package com.SoundFork.SoundFork.project.service;

import com.SoundFork.SoundFork.common.dto.PageResponse;
import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.common.util.ImageUtils;
import com.SoundFork.SoundFork.mergerequest.repository.MergeRequestRepository;
import com.SoundFork.SoundFork.version.entity.VersionTrack;
import com.SoundFork.SoundFork.project.dto.ProjectResponse;
import com.SoundFork.SoundFork.project.dto.UpdateProjectRequest;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.project.repository.ProjectRepository;
import com.SoundFork.SoundFork.track.entity.Track;
import com.SoundFork.SoundFork.track.repository.TrackRepository;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.version.entity.Version;
import com.SoundFork.SoundFork.version.repository.VersionRepository;
import com.SoundFork.SoundFork.version.repository.VersionTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;
    private final VersionRepository versionRepository;
    private final VersionTrackRepository versionTrackRepository;
    private final MergeRequestRepository mergeRequestRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse create(String title, String description, String genre, MultipartFile cover, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        Project project = Project.builder()
                .title(title)
                .description(description)
                .genre(genre)
                .author(author)
                .build();

        Project savedProject = projectRepository.save(project);

        if (cover != null && !cover.isEmpty()) {
            String coverPath = ImageUtils.saveSquareCover(cover,
                    Paths.get(uploadDir, "covers", savedProject.getId().toString()));
            savedProject.setCoverArtPath(coverPath);
        }

        log.info("Project created: id={}, title='{}', author={}",
                savedProject.getId(), savedProject.getTitle(), author.getUsername());

        return buildProjectResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        return buildProjectResponse(project);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<ProjectResponse> getAll(Pageable pageable) {
        Page<Project> page = projectRepository.findAll(pageable);
        return PageResponse.from(page.map(this::buildProjectResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> getByAuthorId(Long authorId, Pageable pageable) {
        if (!userRepository.existsById(authorId)) {
            throw new UserNotFoundException("Author with id " + authorId + " not found");
        }

        Page<Project> page = projectRepository.findByAuthorId(authorId, pageable);
        return PageResponse.from(page.map(this::buildProjectResponse));
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse update(Long id, UpdateProjectRequest request, String username) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
        if (!project.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the project author can update the project");
        }

        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getGenre() != null) {
            project.setGenre(request.getGenre());
        }
        if (request.getCoverArtPath() != null) {
            project.setCoverArtPath(request.getCoverArtPath());
        }
        log.info("Project updated: id={}", id);
        return buildProjectResponse(project);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse uploadCover(Long projectId, MultipartFile file, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
        if (!project.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the project author can update the cover");
        }

        String coverPath = ImageUtils.saveSquareCover(file,
                Paths.get(uploadDir, "covers", projectId.toString()));
        project.setCoverArtPath(coverPath);

        log.info("Cover updated: id={}", projectId);
        return buildProjectResponse(project);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectResponse fork(Long projectId, String username) {
        Project source = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "User " + username + " not found"
                ));

        Project fork = Project.builder()
                .title(source.getTitle() + " (fork)")
                .description(source.getDescription())
                .genre(source.getGenre())
                .author(author)
                .sourceProject(source)
                .coverArtPath(source.getCoverArtPath())
                .build();

        Project savedFork = projectRepository.save(fork);

        if (source.getCoverArtPath() != null && !source.getCoverArtPath().isEmpty()) {
            try {
                Path sourceCover = Paths.get(source.getCoverArtPath());
                if (Files.exists(sourceCover)) {
                    Path destDir = Paths.get(uploadDir, "covers", savedFork.getId().toString());
                    Files.createDirectories(destDir);
                    String fileName = sourceCover.getFileName().toString();
                    Path destPath = destDir.resolve(fileName);
                    Files.copy(sourceCover, destPath, StandardCopyOption.REPLACE_EXISTING);
                    savedFork.setCoverArtPath("covers/" + savedFork.getId() + "/" + fileName);
                    projectRepository.save(savedFork);
                }
            } catch (Exception e) {
                log.warn("Failed to copy cover image on fork: {}", e.getMessage());
            }
        }

        List<Track> sourceTracks = trackRepository
                .findByProjectIdOrderByIdAsc(projectId);
        Map<Long, Long> trackIdMap = new HashMap<>();
        for (Track track : sourceTracks) {
            Track trackCopy = Track.builder()
                    .title(track.getTitle())
                    .project(savedFork)
                    .filePath(track.getFilePath())
                    .fileSize(track.getFileSize())
                    .fileFormat(track.getFileFormat())
                    .bpm(track.getBpm())
                    .musicalKey(track.getMusicalKey())
                    .build();
            Track saved = trackRepository.save(trackCopy);
            trackIdMap.put(track.getId(), saved.getId());
        }

        List<Version> sourceVersions = versionRepository
                .findByProjectIdOrderByVersionNumberAsc(projectId);
        for (Version v : sourceVersions) {
            Version versionCopy = Version.builder()
                    .project(savedFork)
                    .versionNumber(v.getVersionNumber())
                    .commitMessage(v.getCommitMessage())
                    .author(v.getAuthor())
                    .build();
            Version savedVersion = versionRepository.save(versionCopy);

            List<VersionTrack> vts = versionTrackRepository
                    .findByVersionIdOrderByTrackOrderAsc(v.getId());
            for (VersionTrack vt : vts) {
                Long newTrackId = trackIdMap.get(vt.getTrack().getId());
                if (newTrackId == null) continue;
                Track newTrack = trackRepository.findById(newTrackId).orElse(null);
                if (newTrack == null) continue;
                VersionTrack vtCopy = VersionTrack.builder()
                        .version(savedVersion)
                        .track(newTrack)
                        .trackOrder(vt.getTrackOrder())
                        .build();
                versionTrackRepository.save(vtCopy);
            }
        }

        savedFork.setForkedAt(LocalDateTime.now());
        projectRepository.save(savedFork);

        log.info("Fork created: projectId={}, forkId={}, user={}",
                projectId, savedFork.getId(), username);

        return buildProjectResponse(savedFork);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void delete(Long id, String username) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
        if (!project.getAuthor().getId().equals(user.getId()) &&
                user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the project author or an admin can delete this project");
        }

        deleteInternal(id);
    }

    @Transactional
    public void deleteInternal(Long id) {
        List<Long> forkIds = projectRepository.findIdsBySourceProjectId(id);
        for (Long forkId : forkIds) {
            deleteInternal(forkId);
        }
        List<Long> versionIds = versionRepository.findIdsByProjectId(id);
        if (!versionIds.isEmpty()) {
            versionTrackRepository.deleteByVersionIdIn(versionIds);
            mergeRequestRepository.deleteBySourceVersionIdIn(versionIds);
        }
        mergeRequestRepository.deleteByTargetProjectId(id);
        versionRepository.deleteByProjectId(id);
        trackRepository.deleteByProjectId(id);

        projectRepository.deleteById(id);

        log.info("Project id={} deleted", id);
    }

    private ProjectResponse buildProjectResponse(Project project) {
        Long sourceId = null;
        String sourceTitle = null;
        if (project.getSourceProject() != null) {
            sourceId = project.getSourceProject().getId();
            sourceTitle = project.getSourceProject().getTitle();
        }
        Long latestTrackId = null;
        String latestTrackTitle = null;
        try {
            java.util.Optional<Version> optLv = versionRepository
                    .findTopByProjectIdOrderByVersionNumberDesc(project.getId());
            if (optLv.isPresent()) {
                List<VersionTrack> vts = versionTrackRepository
                        .findByVersionIdOrderByTrackOrderAsc(optLv.get().getId());
                if (!vts.isEmpty()) {
                    Track t = vts.get(0).getTrack();
                    latestTrackId = t.getId();
                    latestTrackTitle = t.getTitle();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to resolve latest track for project {}: {}", project.getId(), e.getMessage());
        }

        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .genre(project.getGenre())
                .coverArtPath(project.getCoverArtPath())
                .author(ProjectResponse.AuthorInfo.builder()
                        .id(project.getAuthor().getId())
                        .username(project.getAuthor().getUsername())
                        .build())
                .sourceProjectId(sourceId)
                .sourceProjectTitle(sourceTitle)
                .forkedAt(project.getForkedAt())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .latestTrackId(latestTrackId)
                .latestTrackTitle(latestTrackTitle)
                .build();
    }
}
