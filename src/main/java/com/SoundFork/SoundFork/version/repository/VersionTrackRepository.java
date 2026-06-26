package com.SoundFork.SoundFork.version.repository;

import com.SoundFork.SoundFork.version.entity.VersionTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VersionTrackRepository extends JpaRepository<VersionTrack, Long> {

    List<VersionTrack> findByVersionIdOrderByTrackOrderAsc(Long versionId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM VersionTrack vt WHERE vt.version.id = :versionId")
    void deleteByVersionId(Long versionId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM VersionTrack vt WHERE vt.version.id IN :versionIds")
    void deleteByVersionIdIn(List<Long> versionIds);
}
