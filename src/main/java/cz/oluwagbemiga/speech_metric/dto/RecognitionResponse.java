package cz.oluwagbemiga.speech_metric.dto;

import java.util.UUID;

/**
 * Response DTO returned after a speech recognition operation.
 * <p>
 * Encapsulates the identifier of the persistence entity, the model used,
 * the recognized transcript, the expected transcript (if any) and
 * the computed accuracy metric (e.g. CER-based value in range [0,1]).
 * </p>
 *
 * @param resultId       unique identifier of the recognition result entity
 * @param modelName      logical or file-derived name of the speech model
 * @param recognizedText transcript produced by the engine
 * @param expectedText   ground-truth text supplied by the client (may be null/blank)
 * @param accuracy       normalized accuracy score for the recognition
 */
public record RecognitionResponse(UUID resultId, String modelName, String recognizedText, String expectedText,
                                  double accuracy) {
}
