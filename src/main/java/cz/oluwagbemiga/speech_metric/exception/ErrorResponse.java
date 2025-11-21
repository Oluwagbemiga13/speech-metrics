package cz.oluwagbemiga.speech_metric.exception;

import java.time.LocalDateTime;

/**
 * Standardized error payload returned by the REST layer when exceptions are handled.
 * Immutable record capturing HTTP status code, human readable message and a timestamp.
 * Use the two-argument convenience constructor to auto-populate the timestamp with {@code LocalDateTime.now()}.
 *
 * @param status    HTTP status code
 * @param message   error description
 * @param timestamp creation time of this response (usually server time)
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
