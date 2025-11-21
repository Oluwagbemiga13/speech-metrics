package cz.oluwagbemiga.speech_metric.exception;

/**
 * Exception thrown when a requested speech recognition engine name does not
 * correspond to any configured {@code SpeechEngine} instance.
 */
public class EngineNotFound extends RuntimeException {

    public EngineNotFound(String engineName) {
        super("Engine not found: " + engineName);
    }
}
