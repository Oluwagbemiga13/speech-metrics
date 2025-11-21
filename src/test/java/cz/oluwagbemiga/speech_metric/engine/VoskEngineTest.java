package cz.oluwagbemiga.speech_metric.engine;

import org.junit.jupiter.api.Test;


class VoskEngineTest {

    @Test
    void testPath() {
        String pathToModel = "/models/vosk-model-small-en-us-0.15.bin";
        String originalName = pathToModel
                .replace("\\", "/")
                .substring(pathToModel.lastIndexOf("/") + 1)
                .replace(".bin", "");
        System.out.println(originalName);


    }


}