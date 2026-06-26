package com.SoundFork.SoundFork.user.service;

import com.SoundFork.SoundFork.mergerequest.repository.MergeRequestRepository;
import com.SoundFork.SoundFork.project.repository.ProjectRepository;
import com.SoundFork.SoundFork.track.entity.Track;
import com.SoundFork.SoundFork.track.repository.TrackRepository;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.version.repository.VersionRepository;
import com.SoundFork.SoundFork.version.repository.VersionTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCleanupService {

    private final ProjectRepository projectRepository;
    private final TrackRepository trackRepository;
    private final VersionRepository versionRepository;
    private final VersionTrackRepository versionTrackRepository;
    private final MergeRequestRepository mergeRequestRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Transactional
    public void deleteUserWithData(User user) {
        Long id = user.getId();
        List<Long> ownedProjectIds = projectRepository.findIdsByAuthorId(id);
        List<Long> externalVersionIds = versionRepository.findIdsByAuthorIdNotInProjects(id, ownedProjectIds);
        if (!externalVersionIds.isEmpty()) {
            versionTrackRepository.deleteByVersionIdIn(externalVersionIds);
            mergeRequestRepository.deleteBySourceVersionIdIn(externalVersionIds);
            versionRepository.deleteByIdIn(externalVersionIds);
        }

        for (Long pid : ownedProjectIds) {
            deleteProjectFiles(pid);
        }

        for (Long pid : ownedProjectIds) {
            List<Long> versionIds = versionRepository.findIdsByProjectId(pid);
            if (!versionIds.isEmpty()) {
                versionTrackRepository.deleteByVersionIdIn(versionIds);
                mergeRequestRepository.deleteBySourceVersionIdIn(versionIds);
            }
            mergeRequestRepository.deleteByTargetProjectId(pid);
            versionRepository.deleteByProjectId(pid);
            trackRepository.deleteByProjectId(pid);
            projectRepository.deleteById(pid);
        }

        deleteAvatarFile(user.getAvatarPath());
        mergeRequestRepository.deleteByAuthorId(id);
    }

    private void deleteProjectFiles(Long projectId) {
        List<String> coverPaths = projectRepository.findCoverArtPathById(projectId);
        for (String path : coverPaths) {
            if (path != null) deleteFile(Paths.get(uploadDir, path));
        }

        List<Track> tracks = trackRepository.findByProjectIdOrderByIdAsc(projectId);
        for (Track t : tracks) {
            deleteFile(Paths.get(t.getFilePath()));
        }

        Path coverDir = Paths.get(uploadDir, "covers", projectId.toString());
        deleteDir(coverDir);
    }

    private void deleteAvatarFile(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) return;
        deleteFile(Paths.get(avatarPath));
    }

    private void deleteFile(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { log.warn("Could not delete file: {}", path, e); }
    }

    private void deleteDir(Path path) {
        try { Files.deleteIfExists(path); }
        catch (IOException e) { }
    }
}
