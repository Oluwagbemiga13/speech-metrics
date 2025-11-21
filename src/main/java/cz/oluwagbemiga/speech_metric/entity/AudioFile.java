package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity storing raw uploaded audio data and metadata.
 * Associates to a {@link User} owner and holds {@link RecognitionResult} records
 * produced by recognition engines. Audio bytes are stored as a LOB.
 */
@Entity
@Table(name = "audio_files")
@Data
public class AudioFile {

    /**
     * Primary identifier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Original filename supplied on upload.
     */
    private String fileName;

    /**
     * Creation timestamp (UTC/local per JVM).
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Raw WAV (or other) bytes; non-null.
     */
    @Lob
    @Column(nullable = false)
    private byte[] data;

    /**
     * Owning user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /**
     * Recognition results generated for this audio file.
     */
    @OneToMany(mappedBy = "audioFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecognitionResult> recognitionResults = new ArrayList<>();

}
