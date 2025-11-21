package cz.oluwagbemiga.speech_metric.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Centralized Spring MVC exception handler converting domain exceptions to {@link ErrorResponse} payloads.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles missing user scenarios.
     *
     * @param ex thrown {@link UserNotExistException}
     * @return standardized NOT_FOUND error response
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleUserNotExistException(UserNotExistException ex) {
        log.error("Invalid request: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles missing audio file scenarios.
     *
     * @param ex thrown {@link FileNotExist}
     * @return standardized NOT_FOUND error response
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleFileNotExistException(FileNotExist ex) {
        log.error("Invalid request: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }


}
