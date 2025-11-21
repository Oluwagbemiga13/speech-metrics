package cz.oluwagbemiga.speech_metric.engine;

import com.fasterxml.jackson.databind.JsonNode;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import lombok.extern.slf4j.Slf4j;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
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
            log.error("Vosk recognition failed for model '{}' and audioFile '{}': {}", name, audioFile.getId(), e.toString(), e);
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


    private String recognizeSpeechFromBytes(byte[] data) throws UnsupportedAudioFileException, IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Empty audio data");
        }
        try (AudioInputStream pcm = decodeToPcmMono16k(data);
             Recognizer recognizer = new Recognizer(model, TARGET_SAMPLE_RATE)) {

            AudioFormat pcmFormat = pcm.getFormat();
            log.debug("PCM format fed to recognizer: {} Hz, {} ch, enc={}, sampleSize={} bits", pcmFormat.getSampleRate(), pcmFormat.getChannels(), pcmFormat.getEncoding(), pcmFormat.getSampleSizeInBits());

            byte[] buffer = new byte[4096];
            int n;
            while ((n = pcm.read(buffer)) >= 0) {
                if (n > 0) {
                    recognizer.acceptWaveForm(buffer, n);
                }
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
                log.warn("Failed to parse recognizer JSON, returning raw: {}", e.toString());
                return json; // fallback raw json
            }
        }
    }
}
