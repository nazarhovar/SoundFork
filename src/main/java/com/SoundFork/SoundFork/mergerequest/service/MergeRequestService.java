package com.SoundFork.SoundFork.mergerequest.service;

import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import com.SoundFork.SoundFork.common.exception.UserNotFoundException;
import com.SoundFork.SoundFork.mergerequest.dto.CreateMergeRequest;
import com.SoundFork.SoundFork.mergerequest.dto.MergeRequestResponse;
import com.SoundFork.SoundFork.mergerequest.entity.MergeRequest;
import com.SoundFork.SoundFork.mergerequest.repository.MergeRequestRepository;
import com.SoundFork.SoundFork.notification.event.EventPublisher;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.project.repository.ProjectRepository;
import com.SoundFork.SoundFork.project.service.ProjectNotFoundException;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.user.repository.UserRepository;
import com.SoundFork.SoundFork.version.entity.Version;
import com.SoundFork.SoundFork.version.entity.VersionTrack;
import com.SoundFork.SoundFork.version.repository.VersionRepository;
import com.SoundFork.SoundFork.version.repository.VersionTrackRepository;
import com.SoundFork.SoundFork.version.service.VersionNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeRequestService {

    private final MergeRequestRepository mergeRequestRepository;
    private final VersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final VersionTrackRepository versionTrackRepository;
    private final EventPublisher eventPublisher;
    private final MergeRequestApprovalService approvalService;

    @Transactional
    public MergeRequestResponse create(Long targetProjectId, CreateMergeRequest request, String username) {
        Version sourceVersion = versionRepository.findById(request.getSourceVersionId())
                .orElseThrow(() -> new VersionNotFoundException(request.getSourceVersionId()));

        Project targetProject = projectRepository.findById(targetProjectId)
                .orElseThrow(() -> new ProjectNotFoundException(targetProjectId));

        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        Project sourceProject = sourceVersion.getProject();
        if (!sourceProject.getAuthor().getId().equals(author.getId())) {
            throw new IllegalStateException("Only the version's project owner can submit a merge request");
        }

        if (sourceProject.getSourceProject() == null ||
                !sourceProject.getSourceProject().getId().equals(targetProject.getId())) {
            throw new IllegalStateException("Source project is not a fork of the target project");
        }

        if (mergeRequestRepository.existsBySourceVersionId(sourceVersion.getId())) {
            throw new IllegalStateException("This version already has a merge request");
        }

        List<VersionTrack> sourceTracks = versionTrackRepository
                .findByVersionIdOrderByTrackOrderAsc(sourceVersion.getId());
        if (sourceTracks.isEmpty()) {
            throw new IllegalStateException("Cannot submit merge request: version has no tracks. Upload files first.");
        }

        MergeRequest mr = MergeRequest.builder()
                .sourceVersion(sourceVersion)
                .targetProject(targetProject)
                .author(author)
                .status(MergeRequestStatus.PENDING)
                .message(request.getMessage())
                .build();

        MergeRequest saved = mergeRequestRepository.save(mr);

        eventPublisher.publishNotification(
                targetProject.getAuthor().getId(),
                "New merge request from " + username,
                "MERGE_REQUEST_PENDING",
                targetProject.getId(),
                targetProject.getTitle()
        );

        log.info("Merge request created: id={}, targetProjectId={}, from={}",
                saved.getId(), targetProjectId, username);

        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MergeRequestResponse> getPendingByProject(Long targetProjectId) {
        if (!projectRepository.existsById(targetProjectId)) {
            throw new ProjectNotFoundException(targetProjectId);
        }
        return mergeRequestRepository.findByTargetProjectIdOrderByCreatedAtDesc(targetProjectId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MergeRequestResponse getById(Long id) {
        MergeRequest mr = mergeRequestRepository.findById(id)
                .orElseThrow(() -> new MergeRequestNotFoundException(id));
        return buildResponse(mr);
    }

    @Transactional
    public MergeRequestResponse approve(Long id, String username) {
        MergeRequest mr = mergeRequestRepository.findById(id)
                .orElseThrow(() -> new MergeRequestNotFoundException(id));

        if (MergeRequestStatus.PENDING != mr.getStatus()) {
            throw new IllegalStateException("Merge request is not pending");
        }

        User targetAuthor = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        if (!mr.getTargetProject().getAuthor().getId().equals(targetAuthor.getId())) {
            throw new IllegalStateException("Only the project author can approve merge requests");
        }

        List<VersionTrack> sourceTracks = versionTrackRepository
                .findByVersionIdOrderByTrackOrderAsc(mr.getSourceVersion().getId());

        if (sourceTracks.isEmpty()) {
            throw new IllegalStateException("Source version has no tracks");
        }

        MergeRequest saved = approvalService.approve(mr, username);
        return buildResponse(saved);
    }

    @Transactional
    public MergeRequestResponse reject(Long id, String username) {
        MergeRequest mr = mergeRequestRepository.findById(id)
                .orElseThrow(() -> new MergeRequestNotFoundException(id));

        if (MergeRequestStatus.PENDING != mr.getStatus()) {
            throw new IllegalStateException("Merge request is not pending");
        }

        User targetAuthor = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        if (!mr.getTargetProject().getAuthor().getId().equals(targetAuthor.getId())) {
            throw new IllegalStateException("Only the project author can reject merge requests");
        }

        mr.setStatus(MergeRequestStatus.REJECTED);
        MergeRequest saved = mergeRequestRepository.save(mr);

        eventPublisher.publishNotification(
                mr.getAuthor().getId(),
                "Your merge request was rejected by " + username,
                "MERGE_REQUEST_REJECTED",
                mr.getTargetProject().getId(),
                mr.getTargetProject().getTitle()
        );

        log.info("Merge request rejected: id={}, projectId={}", id, mr.getTargetProject().getId());

        return buildResponse(saved);
    }

    @Transactional(readOnly = true)
    public long getPendingCountForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User " + username + " not found"));

        List<Long> projectIds = projectRepository.findIdsByAuthorId(user.getId());
        if (projectIds.isEmpty()) return 0;

        return mergeRequestRepository.countByTargetProjectIdInAndStatus(
                projectIds, MergeRequestStatus.PENDING);
    }

    private MergeRequestResponse buildResponse(MergeRequest mr) {
        return MergeRequestResponse.builder()
                .id(mr.getId())
                .sourceVersionId(mr.getSourceVersion().getId())
                .sourceVersionNumber(mr.getSourceVersion().getVersionNumber())
                .targetProjectId(mr.getTargetProject().getId())
                .targetProjectTitle(mr.getTargetProject().getTitle())
                .authorId(mr.getAuthor().getId())
                .authorUsername(mr.getAuthor().getUsername())
                .status(mr.getStatus())
                .message(mr.getMessage())
                .createdAt(mr.getCreatedAt())
                .updatedAt(mr.getUpdatedAt())
                .build();
    }
}
