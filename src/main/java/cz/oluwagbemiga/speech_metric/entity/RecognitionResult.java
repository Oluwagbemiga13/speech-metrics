package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class RecognitionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String modelName;
    private String recognizedText;
    private String expectedText;
    private double accuracy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_file_id", nullable = false)
    private AudioFile audioFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_suite_id")
    private RecognitionSuite recognitionSuite;
}
