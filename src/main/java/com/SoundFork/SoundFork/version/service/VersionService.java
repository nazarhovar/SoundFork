package com.SoundFork.SoundFork.version.service;

import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import com.SoundFork.SoundFork.common.enums.Role;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.mergerequest.repository.MergeRequestRepository;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.project.repository.ProjectRepository;
import com.SoundFork.SoundFork.project.service.ProjectNotFoundException;
import com.SoundFork.SoundFork.track.dto.TrackResponse;
import com.SoundFork.SoundFork.track.entity.Track;
import com.SoundFork.SoundFork.track.repository.TrackRepository;
import com.SoundFork.SoundFork.track.service.TrackService;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.version.dto.CreateVersionRequest;
import com.SoundFork.SoundFork.version.dto.VersionResponse;
import com.SoundFork.SoundFork.version.dto.VersionTrackResponse;
import com.SoundFork.SoundFork.version.entity.Version;
import com.SoundFork.SoundFork.version.entity.VersionTrack;
import com.SoundFork.SoundFork.version.repository.VersionRepository;
import com.SoundFork.SoundFork.version.repository.VersionTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {

    private final VersionRepository versionRepository;
    private final VersionTrackRepository versionTrackRepository;
    private final ProjectRepository projectRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final TrackService trackService;
    private final MergeRequestRepository mergeRequestRepository;

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public VersionResponse create(Long projectId, CreateVersionRequest request, String username) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        if (!project.getAuthor().getUsername().equals(username)) {
            throw new IllegalStateException("Only the project author can create versions. Fork the project first.");
        }

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        Version previousVersion = versionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(projectId)
                .orElse(null);

        int nextNumber = (previousVersion == null) ? 1 : previousVersion.getVersionNumber() + 1;

        Version version = Version.builder()
                .project(project)
                .parentVersion(previousVersion)
                .versionNumber(nextNumber)
                .commitMessage(request.getCommitMessage())
                .author(author)
                .build();

        Version savedVersion = versionRepository.save(version);

        log.info("Created empty version {} of project {} (no tracks)", nextNumber, project.getTitle());

        return buildVersionResponse(savedVersion);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public VersionResponse createWithTrack(Long projectId, CreateVersionRequest request, String username,
                                           MultipartFile file, String trackTitle, Integer bpm, String musicalKey) {
        TrackResponse trackResponse = trackService.upload(projectId, file, trackTitle, bpm, musicalKey, username);
        log.info("Track id={} uploaded to project {} during version creation", trackResponse.getId(), projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        Version previousVersion = versionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(projectId)
                .orElse(null);

        int nextNumber = (previousVersion == null) ? 1 : previousVersion.getVersionNumber() + 1;

        Version version = Version.builder()
                .project(project)
                .parentVersion(previousVersion)
                .versionNumber(nextNumber)
                .commitMessage(request.getCommitMessage())
                .author(author)
                .build();

        Version savedVersion = versionRepository.save(version);

        Track track = trackRepository.getReferenceById(trackResponse.getId());

        VersionTrack vt = VersionTrack.builder()
                .version(savedVersion)
                .track(track)
                .trackOrder(1)
                .build();
        versionTrackRepository.save(vt);

        log.info("Created version {} of project {} with new track id={}", nextNumber, project.getTitle(), track.getId());

        return buildVersionResponse(savedVersion);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> getVersionHistory(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        return versionRepository.findByProjectIdOrderByVersionNumberDesc(projectId)
                .stream()
                .map(this::buildVersionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse getById(Long id) {
        Version version = versionRepository.findById(id)
                .orElseThrow(() -> new VersionNotFoundException(id));

        return buildVersionResponse(version);
    }

    @Transactional(readOnly = true)
    public List<VersionTrackResponse> getTracksInVersion(Long versionId) {
        if (!versionRepository.existsById(versionId)) {
            throw new VersionNotFoundException(versionId);
        }

        return versionTrackRepository.findByVersionIdOrderByTrackOrderAsc(versionId)
                .stream()
                .map(this::buildVersionTrackResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void delete(Long id, String username) {
        Version version = versionRepository.findById(id)
                .orElseThrow(() -> new VersionNotFoundException(id));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));
        if (!version.getProject().getAuthor().getId().equals(user.getId()) &&
                user.getRole() != Role.ADMIN) {
            throw new IllegalStateException("Only the project author can delete versions");
        }

        if (mergeRequestRepository.existsBySourceVersionIdAndStatus(id, MergeRequestStatus.APPROVED)) {
            throw new IllegalStateException("Cannot delete a version that has been approved in a merge request");
        }

        Version latest = versionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(version.getProject().getId())
                .orElse(null);
        if (latest == null || !latest.getId().equals(id)) {
            throw new IllegalStateException("Only the latest version can be deleted");
        }

        mergeRequestRepository.deleteBySourceVersionIdIn(List.of(id));
        versionTrackRepository.deleteByVersionId(id);
        versionRepository.deleteById(id);

        log.info("Deleted version id={} from project={}", id, version.getProject().getId());
    }

    private VersionResponse buildVersionResponse(Version version) {
        Long parentId = (version.getParentVersion() != null)
                ? version.getParentVersion().getId()
                : null;

        boolean hasApproved = mergeRequestRepository
                .existsBySourceVersionIdAndStatus(version.getId(), MergeRequestStatus.APPROVED);
        boolean hasAnyMr = mergeRequestRepository
                .existsBySourceVersionId(version.getId());

        return VersionResponse.builder()
                .id(version.getId())
                .projectId(version.getProject().getId())
                .parentVersionId(parentId)
                .versionNumber(version.getVersionNumber())
                .commitMessage(version.getCommitMessage())
                .author(VersionResponse.AuthorInfo.builder()
                        .id(version.getAuthor().getId())
                        .username(version.getAuthor().getUsername())
                        .build())
                .createdAt(version.getCreatedAt())
                .hasApprovedMergeRequest(hasApproved)
                .hasMergeRequest(hasAnyMr)
                .build();
    }

    private VersionTrackResponse buildVersionTrackResponse(VersionTrack vt) {
        Track t = vt.getTrack();
        return VersionTrackResponse.builder()
                .id(vt.getId())
                .trackId(t.getId())
                .trackTitle(t.getTitle())
                .fileFormat(t.getFileFormat())
                .fileSize(t.getFileSize())
                .downloadUrl("/tracks/" + t.getId() + "/download")
                .bpm(t.getBpm())
                .musicalKey(t.getMusicalKey())
                .build();
    }
}
