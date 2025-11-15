package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;


public record RecognitionRequest(AudioFile audioFile, String expectedText) {
}
