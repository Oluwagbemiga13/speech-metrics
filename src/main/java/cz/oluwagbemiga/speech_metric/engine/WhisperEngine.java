package cz.oluwagbemiga.speech_metric.engine;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import io.github.ggerganov.whispercpp.WhisperCpp;
import io.github.ggerganov.whispercpp.params.WhisperFullParams;
import io.github.ggerganov.whispercpp.params.WhisperSamplingStrategy;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whisper.cpp-based engine that mirrors VoskEngine semantics so it can be used interchangeably.
 * <p>
 * Usage: new WhisperEngine("whisper-base", "/abs/path/to/ggml-*.bin")
 */
@Slf4j
public class WhisperEngine extends SpeechEngine {

    // Whisper context cached per model path to allow multiple engines with shared models
    private static final Map<String, WhisperCpp> CTX_CACHE = new ConcurrentHashMap<>();

    private final WhisperCpp whisper;

    public WhisperEngine(String pathToModel) {
        super(pathToModel);

        this.whisper = CTX_CACHE.computeIfAbsent(pathToModel, p -> {
            WhisperCpp w = new WhisperCpp();
            try {
                w.initContext(p);
                return w;
            } catch (IOException e) {
                // Ensure native context is cleaned up on failure
                try {
                    w.close();
                } catch (Exception ignored) {
                }
                throw new IllegalStateException("Failed to load Whisper model at path: " + p, e);
            }
        });
    }


    @Override
    public RecognitionResult processAudio(RecognitionRequest request) {
        AudioFile audioFile = request.audioFile();
        String expected = request.expectedText();
        String recognizedText;
        try {
            float[] samples = toPcmMono16kFloat(audioFile.getData());
            recognizedText = transcribe(samples);
        } catch (Exception e) {
            log.error("Whisper recognition failed for model '{}' and audioFile '{}': {}", name, audioFile.getId(), e.toString(), e);
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

    private String transcribe(float[] samples) throws IOException {
        if (samples == null || samples.length == 0) {
            throw new IOException("Empty audio samples");
        }
        // Use correct type returned by getFullDefaultParams
        WhisperFullParams params = whisper.getFullDefaultParams(WhisperSamplingStrategy.WHISPER_SAMPLING_BEAM_SEARCH);
        // conservative defaults similar to snippet; tune if needed
        params.temperature = 0.0f;
        params.temperature_inc = 0.2f;
        // params.n_threads = Runtime.getRuntime().availableProcessors();
        // params.language = "en"; // set explicitly if you know the language

        return whisper.fullTranscribe(params, samples);
    }

    private float[] toPcmMono16kFloat(byte[] data) throws IOException, UnsupportedAudioFileException {
        if (data == null || data.length == 0) throw new IOException("Empty audio data");
        try (AudioInputStream pcm = decodeToPcmMono16k(data)) {
            byte[] pcmBytes = pcm.readAllBytes();
            // 16-bit little endian PCM -> float [-1,1]
            int samples = pcmBytes.length / 2; // 2 bytes per sample
            float[] out = new float[samples];
            for (int i = 0, s = 0; i + 1 < pcmBytes.length; i += 2, s++) {
                int lo = pcmBytes[i] & 0xFF;
                int hi = pcmBytes[i + 1]; // signed
                int val = (hi << 8) | lo; // little endian
                // sign-extend 16-bit
                short sval = (short) val;
                out[s] = Math.max(-1.0f, Math.min(1.0f, sval / 32767.0f));
            }
            return out;
        }
    }


}
