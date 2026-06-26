package com.SoundFork.SoundFork.mergerequest.entity;

import com.SoundFork.SoundFork.common.enums.MergeRequestStatus;
import com.SoundFork.SoundFork.project.entity.Project;
import com.SoundFork.SoundFork.user.entity.User;
import com.SoundFork.SoundFork.version.entity.Version;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "merge_requests", indexes = {
    @Index(name = "idx_mr_target_project", columnList = "target_project_id"),
    @Index(name = "idx_mr_source_version", columnList = "source_version_id"),
    @Index(name = "idx_mr_author", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class MergeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter(AccessLevel.PACKAGE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_version_id", nullable = false)
    @ToString.Exclude
    private Version sourceVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_project_id", nullable = false)
    @ToString.Exclude
    private Project targetProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    @ToString.Exclude
    private User author;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MergeRequestStatus status = MergeRequestStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
