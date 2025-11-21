package cz.oluwagbemiga.speech_metric.dto;

import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;

import java.util.List;
import java.util.UUID;

/**
 * Aggregated recognition results for a single evaluation suite.
 * <p>
 * Provides a lightweight view of a {@link RecognitionSuite} entity by exposing its id
 * and a list of {@link RecognitionResponse} items derived from the suite's persistent results.
 * </p>
 *
 * @param id                   unique identifier of the recognition suite
 * @param recognitionResponses ordered list of individual recognition responses
 */
public record RecognitionSuiteDTO(
        UUID id,
        List<RecognitionResponse> recognitionResponses
) {

    /**
     * Convenience constructor converting a domain {@link RecognitionSuite} to this DTO.
     * Maps each contained result entity to a {@link RecognitionResponse}.
     *
     * @param suite source domain suite (must be non-null)
     */
    public RecognitionSuiteDTO(RecognitionSuite suite) {
        this(
                suite.getId(),
                suite.getRecognitionResults().stream()
                        .map(result -> new RecognitionResponse(
                                result.getId(),
                                result.getModelName(),
                                result.getRecognizedText(),
                                result.getExpectedText(),
                                result.getAccuracy()
                        ))
                        .toList()
        );
    }
}
