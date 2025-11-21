package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity grouping multiple {@link RecognitionResult} instances produced together
 * for benchmarking or batch evaluation. Provides a logical container so clients
 * can correlate related recognition outcomes (e.g. same audio processed by different models).
 */
@Entity
@Data
public class RecognitionSuite {

    /**
     * Primary identifier (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    private final LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Collection of recognition results associated with this suite.
     */
    @OneToMany(mappedBy = "recognitionSuite", cascade = CascadeType.ALL)
    private List<RecognitionResult> recognitionResults = new ArrayList<>();
}
