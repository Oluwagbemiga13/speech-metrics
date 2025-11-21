package cz.oluwagbemiga.speech_metric.exception;

/**
 * Exception thrown when an uploaded audio file cannot be read, processed or normalized.
 */
public class UploadFileException extends RuntimeException {
    public UploadFileException(String message) {
        super(message);
    }
}
