package cz.oluwagbemiga.speech_metric.exception;

public class FileNotExist extends RuntimeException {

    public FileNotExist(String fileUUID) {
        super("File with UUID " + fileUUID + " does not exist.");
    }
}
