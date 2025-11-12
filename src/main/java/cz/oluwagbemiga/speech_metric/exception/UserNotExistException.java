package cz.oluwagbemiga.speech_metric.exception;

public class UserNotExistException extends RuntimeException {
    public UserNotExistException(String username) {
        super("User with username '" + username + "' does not exist.");
    }
}
