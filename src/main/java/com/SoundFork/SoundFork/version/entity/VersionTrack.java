package com.SoundFork.SoundFork.version.entity;

import com.SoundFork.SoundFork.track.entity.Track;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "version_tracks", indexes = {
    @Index(name = "idx_vt_version", columnList = "version_id"),
    @Index(name = "idx_vt_track", columnList = "track_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class VersionTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    @Setter(AccessLevel.PACKAGE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    @ToString.Exclude
    private Version version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    @ToString.Exclude
    private Track track;

    private Integer trackOrder;
}
