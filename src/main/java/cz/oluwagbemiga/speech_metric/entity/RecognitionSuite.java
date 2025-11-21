package cz.oluwagbemiga.speech_metric.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    /**
     * Collection of recognition results associated with this suite.
     */
    @OneToMany(mappedBy = "recognitionSuite", cascade = CascadeType.ALL)
    private List<RecognitionResult> recognitionResults;
}
