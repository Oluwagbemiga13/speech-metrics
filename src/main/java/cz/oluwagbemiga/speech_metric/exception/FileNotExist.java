package cz.oluwagbemiga.speech_metric.exception;

/**
 * Exception indicating that an audio file referenced by UUID was not found in persistence.
 */
public class FileNotExist extends RuntimeException {

    public FileNotExist(String fileUUID) {
        super("File with UUID " + fileUUID + " does not exist.");
    }
}
