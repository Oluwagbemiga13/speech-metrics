package cz.oluwagbemiga.speech_metric.exception;

import java.time.LocalDateTime;

/**
 * Represents an error response for exceptions in the Fairy Tales application.
 *
 * @param status
 * @param message
 * @param timestamp
 */
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now());
    }

}
