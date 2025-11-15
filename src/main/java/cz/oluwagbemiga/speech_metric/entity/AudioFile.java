package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audio_files")
@Data
public class AudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fileName;

    private LocalDateTime createdAt = LocalDateTime.now();

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "audioFile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecognitionResult> recognitionResults = new ArrayList<>();

}
