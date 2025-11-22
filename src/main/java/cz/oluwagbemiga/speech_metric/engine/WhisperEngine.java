package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SpeechEngine implementation backed by the native {@code whisper.cpp} library.
 * <p>
 * This engine mirrors the semantics of {@link VoskEngine} so that callers can
 * switch engines transparently when evaluating recognition quality. A single
 * native Whisper context is cached per model path to avoid re-loading large
 * model binaries in memory for multiple engine instances.
 * <p>
 * Typical usage:
 * <pre>
 *     SpeechEngine engine = new WhisperEngine("/abs/path/to/ggml-base.en.bin");
 *     RecognitionResult result = engine.processAudio(request);
 * </pre>
 */
@Slf4j
public class WhisperEngine extends SpeechEngine {

    // Whisper context cached per model path to allow multiple engines with shared models
    private static final Map<String, WhisperCpp> CTX_CACHE = new ConcurrentHashMap<>();

    private final WhisperCpp whisper;

    /**
     * Creates a new WhisperEngine and initializes (or reuses) a native Whisper context.
     *
     * @param pathToModel absolute path to the Whisper ggml model file (e.g. {@code ggml-base.en.bin})
     * @throws IllegalStateException if the model cannot be loaded
     */
    public WhisperEngine(String pathToModel) {
        super(pathToModel);

        this.whisper = CTX_CACHE.computeIfAbsent(pathToModel, p -> {
            WhisperCpp w = new WhisperCpp();
            try {
                log.info("Loading Whisper model context path={}", p);
                w.initContext(p);
                log.info("Whisper model loaded path={}", p);
                return w;
            } catch (IOException e) {
                // Ensure native context is cleaned up on failure
                try {
                    w.close();
                } catch (Exception closeEx) {
                    log.debug("Ignoring Whisper context close failure after init error", closeEx);
                }
                throw new IllegalStateException("Failed to load Whisper model at path: " + p, e);
            }
        });
        if (CTX_CACHE.containsKey(pathToModel)) {
            log.debug("Using cached Whisper context for model={}", name);
        }
    }


    /**
     * Performs full transcription of the audio in the provided request and computes accuracy.
     *
     * @param request recognition request containing audio bytes and expected text
     * @return populated {@link RecognitionResult} including recognized text and CER-based accuracy
     */
    @Override
    public RecognitionResult processAudio(RecognitionRequest request) {
        long startNanos = System.nanoTime();
        AudioFile audioFile = request.audioFile();
        log.debug("WhisperEngine processAudio start audioFile={} dataBytes={}", audioFile.getId(), audioFile.getData() == null ? 0 : audioFile.getData().length);
        String expected = request.expectedText();
        String recognizedText;
        long modelProcessingMs = 0L;
        try {
            float[] samples = toPcmMono16kFloat(audioFile.getData());
            log.trace("Converted WAV to float samples count={}", samples.length);
            long modelStart = System.nanoTime();
            recognizedText = transcribe(samples);
            modelProcessingMs = (System.nanoTime() - modelStart) / 1_000_000L;
        } catch (Exception e) {
            log.error("Whisper recognition failed for model '{}' and audioFile '{}'", name, audioFile.getId(), e);
            recognizedText = ""; // fallback to empty string on failure
        }
        double accuracy = computeAccuracy(expected, recognizedText);
        long totalElapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        log.info("WhisperEngine finished audioFile={} model={} chars={} accuracy={} timeMs={} modelOnlyMs={}", audioFile.getId(), name, recognizedText.length(), accuracy, totalElapsedMs, modelProcessingMs);

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
     * Runs a full Whisper transcription using beam search sampling.
     *
     * @param samples PCM mono 16 kHz float samples in range [-1, 1]
     * @return recognized text from the Whisper model
     * @throws IOException if samples are empty or transcription fails at native level
     */
    private String transcribe(float[] samples) throws IOException {
        if (samples == null || samples.length == 0) {
            throw new IOException("Empty audio samples");
        }
        log.debug("Starting Whisper transcription samples={} model={}", samples.length, name);
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        params.temperature = 0.0f;
        params.temperature_inc = 0.2f;
        String text = whisper.fullTranscribe(params, samples);
        log.debug("Completed Whisper transcription model={} textLength={}", name, text == null ? 0 : text.length());
        return text;
    }

    /**
     * Converts a WAV (PCM s16le mono 16 kHz) byte array to normalized float samples [-1,1].
     *
     * @param wav full WAV container bytes
     * @return float array of audio samples
     * @throws IOException if the WAV data is invalid or empty
     */
    private float[] toPcmMono16kFloat(byte[] wav) throws IOException {
        if (wav == null || wav.length == 0) throw new IOException("Empty audio data");
        byte[] pcmBytes = extractPcmS16leMono16k(wav);
        int samples = pcmBytes.length / 2; // 2 bytes per sample
        float[] out = new float[samples];
        for (int i = 0, s = 0; i + 1 < pcmBytes.length; i += 2, s++) {
            int lo = pcmBytes[i] & 0xFF;
            int hi = pcmBytes[i + 1]; // signed
            int val = (hi << 8) | lo; // little endian
            short sval = (short) val;
            out[s] = Math.max(-1.0f, Math.min(1.0f, sval / 32767.0f));
        }
        return out;
    }
}
