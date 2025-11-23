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
                log.info("Loading Vosk model path={}", p);
                Model m = new Model(p);
                log.info("Vosk model loaded path={}", p);
                return m;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load Vosk model at path: " + p, e);
            }
        });
        if (MODEL_CACHE.containsKey(pathToModel)) {
            log.debug("Using cached Vosk model path={} name={}", pathToModel, name);
        }
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
        long startNanos = System.nanoTime();
        AudioFile audioFile = request.audioFile();
        log.debug("VoskEngine processAudio start audioFile={} dataBytes={}", audioFile.getId(), audioFile.getData() == null ? 0 : audioFile.getData().length);
        String expected = request.expectedText();
        String recognizedText = "";
        long modelProcessingMs = 0L;
        try {
            long[] timeRef = new long[1];
            recognizedText = recognizeSpeechFromBytes(audioFile.getData(), timeRef);
            modelProcessingMs = timeRef[0];
        } catch (Exception e) {
            log.error("Vosk recognition failed for model '{}' and audioFile '{}'", name, audioFile.getId(), e);
        }
        double accuracy = computeAccuracy(expected, recognizedText);
        long totalElapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("VoskEngine finished audioFile={} model={} chars={} accuracy={} timeMs={} modelOnlyMs={}", audioFile.getId(), name, recognizedText.length(), accuracy, totalElapsedMs, modelProcessingMs);

        RecognitionResult result = new RecognitionResult();
        result.setModelName(name);
        result.setRecognizedText(recognizedText);
        result.setExpectedText(expected);
        result.setAccuracy(accuracy);
        result.setModelProcessingTimeMs(modelProcessingMs);
        result.setAudioFile(audioFile);
        result.setOwner(audioFile.getOwner());
        audioFile.getRecognitionResults().add(result);
        return result;
    }


    /**
     * Performs incremental recognition on raw WAV bytes (PCM s16le mono 16 kHz) and returns final text.
     * Falls back to raw JSON if parsing of the recognizer output fails.
     */
    private String recognizeSpeechFromBytes(byte[] data) throws IOException {
        long[] timeRef = new long[1];
        return recognizeSpeechFromBytes(data, timeRef);
    }

    private String recognizeSpeechFromBytes(byte[] data, long[] timeRef) throws IOException {
        if (data == null || data.length == 0) {
            throw new IOException("Empty audio data");
        }
        byte[] pcm = extractPcmS16leMono16k(data);
        if (pcm.length == 0) {
            throw new IOException("No PCM audio extracted");
        }
        int total = pcm.length;
        log.debug("Starting Vosk streaming recognition pcmBytes={} model={}", total, name);
        long modelStart = System.nanoTime();
        try (Recognizer recognizer = new Recognizer(model, TARGET_SAMPLE_RATE)) {
            int chunkSize = 4096;
            byte[] buffer = new byte[chunkSize];
            int offset = 0;
            int chunkCount = 0;
            while (offset < total) {
                int len = Math.min(chunkSize, total - offset);
                System.arraycopy(pcm, offset, buffer, 0, len);
                recognizer.acceptWaveForm(buffer, len);
                offset += len;
                chunkCount++;
            }
            long modelMs = (System.nanoTime() - modelStart) / 1_000_000L;
            if (timeRef != null && timeRef.length > 0) {
                timeRef[0] = modelMs;
            }
            int lastChunkSize = total % chunkSize == 0 ? chunkSize : total % chunkSize;
            log.trace("Vosk streaming complete chunks={} lastChunkSize={} totalBytes={} model={} modelMs={}", chunkCount, lastChunkSize, total, name, modelMs);
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
                return json;
            }
        }
    }
}
