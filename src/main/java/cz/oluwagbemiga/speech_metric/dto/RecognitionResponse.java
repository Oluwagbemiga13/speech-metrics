package cz.oluwagbemiga.speech_metric.dto;

import java.util.UUID;

public record RecognitionResponse(UUID resultId, String modelName, String recognizedText, String expectedText,
                                  double accuracy) {
}
