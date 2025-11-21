package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

/**
 * JPA entity representing a single speech recognition outcome for an audio file.
 * Stores model metadata, transcripts (recognized vs expected) and an accuracy metric.
 * Links to the originating {@link AudioFile}, the {@link User} owner and optionally
 * a {@link RecognitionSuite} for grouped evaluations.
 */
@Entity
@Data
public class RecognitionResult {

    /**
     * Primary identifier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Logical/model file name of the speech engine used.
     */
    private String modelName;
    /**
     * Transcript produced by the recognition engine.
     */
    private String recognizedText;
    /**
     * Ground-truth transcript (if provided).
     */
    private String expectedText;
    /**
     * Accuracy score (e.g. CER-based) in range [0,1].
     */
    private double accuracy;

    /**
     * Source audio file for this recognition.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_file_id", nullable = false)
    private AudioFile audioFile;

    /**
     * Owning user initiating or associated with the recognition.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /**
     * Optional grouping suite for batch evaluation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recognition_suite_id")
    private RecognitionSuite recognitionSuite;
}
