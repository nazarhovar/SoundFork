package com.SoundFork.SoundFork.mergerequest.service;

import com.SoundFork.SoundFork.common.email.EmailService;
import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import com.SoundFork.SoundFork.mergerequest.entity.MergeRequest;
import com.SoundFork.SoundFork.mergerequest.repository.MergeRequestRepository;
import com.SoundFork.SoundFork.notification.event.EventPublisher;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.track.entity.Track;
import com.SoundFork.SoundFork.track.repository.TrackRepository;
import com.SoundFork.SoundFork.version.entity.Version;
import com.SoundFork.SoundFork.version.entity.VersionTrack;
import com.SoundFork.SoundFork.version.repository.VersionRepository;
import com.SoundFork.SoundFork.version.repository.VersionTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeRequestApprovalService {

    private final MergeRequestRepository mergeRequestRepository;
    private final VersionRepository versionRepository;
    private final VersionTrackRepository versionTrackRepository;
    private final TrackRepository trackRepository;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public MergeRequest approve(MergeRequest mr, String username) {
        Project targetProject = mr.getTargetProject();
        Version sourceVersion = mr.getSourceVersion();

        List<VersionTrack> sourceTracks = versionTrackRepository
                .findByVersionIdOrderByTrackOrderAsc(sourceVersion.getId());

        Version previousVersion = versionRepository
                .findTopByProjectIdOrderByVersionNumberDesc(targetProject.getId())
                .orElse(null);

        int nextNumber = (previousVersion == null) ? 1 : previousVersion.getVersionNumber() + 1;

        Version newVersion = Version.builder()
                .project(targetProject)
                .parentVersion(previousVersion)
                .versionNumber(nextNumber)
                .commitMessage(mr.getMessage())
                .author(mr.getAuthor())
                .build();

        Version savedVersion = versionRepository.save(newVersion);

        for (VersionTrack vt : sourceTracks) {
            Track sourceTrack = vt.getTrack();

            Track trackCopy = Track.builder()
                    .title(sourceTrack.getTitle())
                    .project(targetProject)
                    .filePath(sourceTrack.getFilePath())
                    .fileSize(sourceTrack.getFileSize())
                    .fileFormat(sourceTrack.getFileFormat())
                    .bpm(sourceTrack.getBpm())
                    .musicalKey(sourceTrack.getMusicalKey())
                    .build();

            Track savedTrack = trackRepository.save(trackCopy);

            VersionTrack versionTrack = VersionTrack.builder()
                    .version(savedVersion)
                    .track(savedTrack)
                    .trackOrder(vt.getTrackOrder())
                    .build();

            versionTrackRepository.save(versionTrack);
        }

        mr.setStatus(MergeRequestStatus.APPROVED);
        MergeRequest saved = mergeRequestRepository.save(mr);

        eventPublisher.publishNotification(
                mr.getAuthor().getId(),
                "Your merge request (v" + sourceVersion.getVersionNumber() + ") was approved by " + username,
                "MERGE_REQUEST_APPROVED",
                targetProject.getId(),
                targetProject.getTitle()
        );

        emailService.sendEmail(
                mr.getAuthor().getEmail(),
                "Merge Request Approved — " + targetProject.getTitle(),
                "Hello " + mr.getAuthor().getUsername() + ",\n\nYour merge request has been approved by " + username
                        + "! Version v" + sourceVersion.getVersionNumber() + " has been merged into \""
                        + targetProject.getTitle() + "\".\n\nThank you for your contribution!"
        );

        log.info("Merge request approved: id={}, projectId={}, versionNumber={}",
                mr.getId(), targetProject.getId(), nextNumber);

        return saved;
    }
}
