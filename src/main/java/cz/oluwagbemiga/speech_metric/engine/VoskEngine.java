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
 * SpeechEngine implementation using the Vosk offline speech recognition library.
 * <p>
 * A {@link Model} instance is cached per model path to prevent redundant loading
 * of large acoustic/language models. The engine exposes a uniform API identical
 * to {@link WhisperEngine} for easy benchmarking.
 * <p>
 * Usage example:
 * <pre>
 *     SpeechEngine engine = new VoskEngine("/abs/path/to/vosk-model-en-us-0.22");
 *     RecognitionResult result = engine.processAudio(request);
 * </pre>
 */
@Slf4j
public class VoskEngine extends SpeechEngine {

    private final Model model;
    private static final Map<String, Model> MODEL_CACHE = new ConcurrentHashMap<>();

    /**
     * Constructs a VoskEngine, reusing a cached model when available.
     *
     * @param pathToModel absolute path to a Vosk model directory
     * @throws IllegalStateException if the model fails to load
     */
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


    /**
     * Streams the audio to a Vosk Recognizer instance and produces a final transcription.
     * Computes character-error-rate accuracy against the expected text.
     *
     * @param request recognition request containing audio data and expected transcript
     * @return {@link RecognitionResult} containing recognized text and accuracy metrics
     */
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


    /**
     * Performs incremental recognition on raw WAV bytes (PCM s16le mono 16 kHz) and returns final text.
     * Falls back to raw JSON if parsing of the recognizer output fails.
     *
     * @param data WAV container bytes
     * @return recognized plain text (or raw recognizer JSON on parse failure)
     * @throws IOException if the input audio data is invalid or empty
     */
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
