package cz.oluwagbemiga.speech_metric.exception;

/**
 * Exception indicating that a user lookup by username/ID failed.
 */
public class UserNotExistException extends RuntimeException {
    public UserNotExistException(String username) {
        super("User with username '" + username + "' does not exist.");
    }
}
