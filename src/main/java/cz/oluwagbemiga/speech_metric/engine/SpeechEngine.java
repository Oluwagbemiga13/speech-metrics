package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;


public interface SpeechEngine {

    String name();

    RecognitionResult processAudio(RecognitionRequest recognitionRequest);

}
