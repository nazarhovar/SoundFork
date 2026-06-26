package com.SoundFork.SoundFork.mergerequest.repository;

import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import com.SoundFork.SoundFork.mergerequest.entity.MergeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MergeRequestRepository extends JpaRepository<MergeRequest, Long> {

    List<MergeRequest> findByTargetProjectIdOrderByCreatedAtDesc(Long targetProjectId);

    List<MergeRequest> findByTargetProjectIdInAndStatus(List<Long> projectIds, MergeRequestStatus status);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MergeRequest m WHERE m.targetProject.id = :targetProjectId")
    void deleteByTargetProjectId(Long targetProjectId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MergeRequest m WHERE m.author.id = :authorId")
    void deleteByAuthorId(Long authorId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MergeRequest m WHERE m.sourceVersion.id IN :versionIds")
    void deleteBySourceVersionIdIn(List<Long> versionIds);

    @Query("SELECT COUNT(m) FROM MergeRequest m WHERE m.targetProject.id IN :projectIds AND m.status = :status")
    long countByTargetProjectIdInAndStatus(List<Long> projectIds, MergeRequestStatus status);

    boolean existsBySourceVersionIdAndStatus(Long sourceVersionId, MergeRequestStatus status);

    boolean existsBySourceVersionId(Long sourceVersionId);
}
