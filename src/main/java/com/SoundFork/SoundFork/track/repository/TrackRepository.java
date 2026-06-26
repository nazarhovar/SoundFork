package com.SoundFork.SoundFork.track.repository;

import com.SoundFork.SoundFork.track.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrackRepository extends JpaRepository<Track, Long> {
    List<Track> findByProjectIdOrderByIdAsc(Long projectId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Track t WHERE t.project.id = :projectId")
    void deleteByProjectId(Long projectId);
}
