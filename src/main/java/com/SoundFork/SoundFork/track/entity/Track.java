package com.SoundFork.SoundFork.track.entity;

import com.SoundFork.SoundFork.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "tracks", indexes = {
    @Index(name = "idx_track_project", columnList = "project_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter(AccessLevel.PACKAGE)
    private Long id;
    @Column(nullable = false)
    private String title;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @ToString.Exclude
    private Project project;
    @Column(nullable = false)
    private String filePath;
    private Long fileSize;
    private String fileFormat;
    private Integer bpm;
    private String musicalKey;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
