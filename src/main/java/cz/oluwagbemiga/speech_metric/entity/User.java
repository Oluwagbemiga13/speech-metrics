package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity representing an application user.
 * <p>
 * Stores a unique username and maintains ownership relations to uploaded {@link AudioFile}
 * entities and generated {@link RecognitionResult} records. Cascade + orphanRemoval ensures
 * user-owned artifacts are cleaned up automatically when a user is removed.
 * </p>
 */
@Entity
@Table(name = "users")
@Data
public class User {
    /**
     * Primary identifier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique, non-null username.
     */
    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    /**
     * Audio files owned by this user.
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AudioFile> audioFiles = new ArrayList<>();

    /**
     * Recognition results generated under this user (across audio files / suites).
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecognitionResult> recognitionResults = new ArrayList<>();

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecognitionSuite> recognitionSuites = new ArrayList<>();

}
