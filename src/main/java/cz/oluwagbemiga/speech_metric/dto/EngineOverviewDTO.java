package cz.oluwagbemiga.speech_metric.dto;

import java.util.List;
import java.util.UUID;

/**
 * Overview DTO for evaluating engine performance.
 * <p>
 * Aggregates all recognition results for a specific engine and provides
 * a total percentage accuracy based on all results processed by that engine.
 * </p>
 *
 * @param engineName             the name of the speech recognition engine
 * @param resultIds              list of all recognition result UUIDs processed by this engine
 * @param totalAccuracyPercentage average accuracy across all results as a percentage (0-100)
 * @param totalResults           total number of recognition results for this engine
 * @param averageProcessingTimeMs average processing time in milliseconds across all results
 */
public record EngineOverviewDTO(
        String engineName,
        List<UUID> resultIds,
        double totalAccuracyPercentage,
        int totalResults,
        double averageProcessingTimeMs
) {
}

