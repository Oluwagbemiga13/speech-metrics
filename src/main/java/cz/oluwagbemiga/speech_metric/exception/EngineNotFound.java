package cz.oluwagbemiga.speech_metric.exception;

public class EngineNotFound extends RuntimeException {


    public EngineNotFound(String engineName) {
        super("Engine not found: " + engineName);
    }
}
