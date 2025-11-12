package cz.oluwagbemiga.speech_metric.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles InvalidRequestException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @return a ResponseEntity containing the error response
     */
    @ExceptionHandler
    public ResponseEntity<ErrorResponse> handleUserNotExistException(UserNotExistException ex) {
        log.error("Invalid request: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }


}
