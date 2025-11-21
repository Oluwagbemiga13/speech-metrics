package cz.oluwagbemiga.speech_metric.dto;

import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;

import java.util.List;
import java.util.UUID;

public record RecognitionSuiteDTO(
        UUID id,
        List<RecognitionResponse> recognitionResponses
) {

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
