package cz.oluwagbemiga.speech_metric.dto;

import java.util.UUID;

/**
 * Response DTO returned after a speech recognition operation.
 * <p>
 * Encapsulates the identifier of the persistence entity, the model used,
 * the recognized transcript, the expected transcript (if any) and
 * the computed accuracy metric (e.g. CER-based value in range [0,1]).
 * Includes modelProcessingTimeMs for underlying inference duration (excluding pre/post processing).
 * </p>
 */
public record RecognitionResponse(
        UUID resultId,
        String modelName,
        String recognizedText,
        String expectedText,
        double accuracy,
        long modelProcessingTimeMs
) {
}
