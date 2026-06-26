package com.SoundFork.SoundFork.project.repository;

import com.SoundFork.SoundFork.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Page<Project> findByAuthorId(Long authorId, Pageable pageable);

    @Query("SELECT p.id FROM Project p WHERE p.sourceProject.id = :sourceProjectId")
    List<Long> findIdsBySourceProjectId(Long sourceProjectId);

    @Query("SELECT p.id FROM Project p WHERE p.author.id = :authorId")
    List<Long> findIdsByAuthorId(Long authorId);

    @Query("SELECT p.coverArtPath FROM Project p WHERE p.id = :id")
    List<String> findCoverArtPathById(Long id);
}


