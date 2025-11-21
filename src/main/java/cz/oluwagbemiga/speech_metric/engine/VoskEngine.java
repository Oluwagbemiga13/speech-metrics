package cz.oluwagbemiga.speech_metric.engine;

import com.fasterxml.jackson.databind.JsonNode;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import lombok.extern.slf4j.Slf4j;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Vosk speech recognition engine implementation.
 * Uses Vosk API to process audio data and return recognized text.
 */
@Slf4j
public class VoskEngine extends SpeechEngine {

    private final Model model;
    private static final Map<String, Model> MODEL_CACHE = new ConcurrentHashMap<>();


    public VoskEngine(String pathToModel) {
        super(pathToModel);
        this.model = MODEL_CACHE.computeIfAbsent(pathToModel, p -> {
            try {
                return new Model(p);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load Vosk model at path: " + p, e);
            }
        });
    }


    @Override
    public RecognitionResult processAudio(RecognitionRequest request) {
        AudioFile audioFile = request.audioFile();
        String expected = request.expectedText();
        String recognizedText;
        try {
            recognizedText = recognizeSpeechFromBytes(audioFile.getData());
        } catch (Exception e) {
            log.error("Vosk recognition failed for model '{}' and audioFile '{}'", name, audioFile.getId(), e);
            recognizedText = ""; // fallback to empty string on failure
        }
        double accuracy = computeAccuracy(expected, recognizedText);

        RecognitionResult result = new RecognitionResult();
        result.setModelName(name);
        result.setRecognizedText(recognizedText);
        result.setExpectedText(expected);
        result.setAccuracy(accuracy);
        result.setAudioFile(audioFile);
        result.setOwner(audioFile.getOwner());
        audioFile.getRecognitionResults().add(result);
        return result;
    }


    private String recognizeSpeechFromBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Empty audio data");
        }
        byte[] pcm = extractPcmS16leMono16k(data);
        try (Recognizer recognizer = new Recognizer(model, TARGET_SAMPLE_RATE)) {
            int offset = 0;
            int chunk = 4096;
            byte[] buf = new byte[chunk];
            while (offset < pcm.length) {
                int len = Math.min(chunk, pcm.length - offset);
                System.arraycopy(pcm, offset, buf, 0, len);
                recognizer.acceptWaveForm(buf, len);
                offset += len;
            }
            String json = recognizer.getFinalResult();
            try {
                JsonNode node = OBJECT_MAPPER.readTree(json);
                String text = node.path("text").asText("");
                if (text.isEmpty()) {
                    log.info("Recognizer returned empty text for model '{}'", name);
                }
                return text;
            } catch (Exception e) {
                log.warn("Failed to parse recognizer JSON, returning raw", e);
                return json; // fallback raw json
            }
        }
    }
}
