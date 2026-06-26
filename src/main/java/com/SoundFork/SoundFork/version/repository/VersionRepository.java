package com.SoundFork.SoundFork.version.repository;

import com.SoundFork.SoundFork.version.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VersionRepository extends JpaRepository<Version, Long> {

    List<Version> findByProjectIdOrderByVersionNumberDesc(Long projectId);

    List<Version> findByProjectIdOrderByVersionNumberAsc(Long projectId);

    Optional<Version> findTopByProjectIdOrderByVersionNumberDesc(Long projectId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Version v WHERE v.project.id = :projectId")
    void deleteByProjectId(Long projectId);

    @Query("SELECT v.id FROM Version v WHERE v.project.id = :projectId")
    List<Long> findIdsByProjectId(Long projectId);

    @Query("SELECT v.id FROM Version v WHERE v.author.id = :authorId AND v.project.id NOT IN :projectIds")
    List<Long> findIdsByAuthorIdNotInProjects(Long authorId, List<Long> projectIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Version v WHERE v.id IN :ids")
    void deleteByIdIn(List<Long> ids);

    List<Version> findByAuthorId(Long authorId);
}
