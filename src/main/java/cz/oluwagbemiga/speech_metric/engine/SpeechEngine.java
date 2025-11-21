package cz.oluwagbemiga.speech_metric.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;


@Slf4j
public abstract class SpeechEngine {

    protected final float TARGET_SAMPLE_RATE = 16000.0f;
    protected final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final String FFMPEG_CMD = System.getenv("FFMPEG_PATH") != null ? System.getenv("FFMPEG_PATH") : "ffmpeg";
    protected final String pathToModel;
    protected String name;

    protected SpeechEngine(String pathToModel) {
        this.pathToModel = pathToModel;
        pathToModel = pathToModel.replace("\\", "/");
        name = pathToModel
                .substring(pathToModel.lastIndexOf("/") + 1)
                .replace(".bin", "");
    }

    public abstract RecognitionResult processAudio(RecognitionRequest recognitionRequest);

    protected AudioInputStream convertToPcmMono16k(AudioInputStream source) {
        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                TARGET_SAMPLE_RATE,
                16,
                1,
                2,
                TARGET_SAMPLE_RATE,
                false
        );
        // First convert to PCM_SIGNED if needed
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(AudioFormat.Encoding.PCM_SIGNED, source);
        // Then convert to desired sample rate / channels
        if (!pcmStream.getFormat().matches(targetFormat)) {
            pcmStream = AudioSystem.getAudioInputStream(targetFormat, pcmStream);
        }
        return pcmStream;
    }

    // Use ffmpeg (must be available on PATH or via FFMPEG_PATH env) to decode any input to 16kHz mono s16le
    protected AudioInputStream decodeWithFfmpeg(byte[] data) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_CMD,
                "-hide_banner",
                "-loglevel", "error",
                "-i", "pipe:0",
                "-f", "s16le",
                "-acodec", "pcm_s16le",
                "-ac", "1",
                "-ar", String.valueOf((int) TARGET_SAMPLE_RATE),
                "pipe:1"
        );
        Process proc;
        try {
            proc = pb.start();
        } catch (IOException ioe) {
            throw new IOException("Failed to start ffmpeg process. Ensure ffmpeg is installed and FFMPEG_PATH is set if not on PATH.", ioe);
        }

        // Write input data to ffmpeg stdin
        try (OutputStream os = proc.getOutputStream()) {
            os.write(data);
        }

        byte[] pcmBytes = proc.getInputStream().readAllBytes();
        try {
            int exitCode = proc.waitFor();
            if (exitCode != 0 || pcmBytes.length == 0) {
                // Try to read error stream for debugging
                String err = new String(proc.getErrorStream().readAllBytes());
                log.error("ffmpeg failed (exit {}) converting audio: {}", exitCode, err);
                throw new IOException("ffmpeg conversion failed, exit=" + exitCode);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for ffmpeg", ie);
        }

        AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                TARGET_SAMPLE_RATE,
                16,
                1,
                2,
                TARGET_SAMPLE_RATE,
                false
        );
        long frameLength = pcmBytes.length / targetFormat.getFrameSize();
        return new AudioInputStream(new ByteArrayInputStream(pcmBytes), targetFormat, frameLength);
    }

    // Attempt JavaSound decode first, then fall back to ffmpeg for unsupported formats like m4a/aac.
    protected AudioInputStream decodeToPcmMono16k(byte[] data) throws IOException, UnsupportedAudioFileException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            try {
                AudioInputStream original = AudioSystem.getAudioInputStream(bais);
                if (original == null) throw new UnsupportedAudioFileException("AudioInputStream is null");
                AudioFormat originalFormat = original.getFormat();
                log.debug("Original audio format: {} Hz, {} ch, enc={}, sampleSize={} bits", originalFormat.getSampleRate(), originalFormat.getChannels(), originalFormat.getEncoding(), originalFormat.getSampleSizeInBits());

                // Convert to PCM mono 16k
                return convertToPcmMono16k(original);
            } catch (UnsupportedAudioFileException uafe) {
                log.info("JavaSound cannot decode input ({}). Falling back to ffmpeg...", uafe.toString());
                // Fall back to ffmpeg for formats like m4a/aac
                return decodeWithFfmpeg(data);
            }
        }
    }

    // Character Error Rate (CER) based accuracy: 1 - (editDistance / expectedCharCount). Returns 0 when expected is empty.
    protected double computeAccuracy(String expected, String recognized) {
        if (expected == null) return 0.0d;
        String exp = normalizeForCer(expected);
        if (exp.isBlank()) return 0.0d;
        String rec = normalizeForCer(recognized == null ? "" : recognized);
        int distance = levenshteinChars(exp, rec);
        return Math.max(0d, 1d - (double) distance / exp.length());
    }

    protected String normalizeForCer(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[\\?\\.,!]", "") // remove ? . , !
                .trim()
                .replaceAll("\\s+", " ");
    }

    protected int levenshteinChars(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[][] dp = new int[n + 1][m + 1];
        for (int i = 0; i <= n; i++) dp[i][0] = i;
        for (int j = 0; j <= m; j++) dp[0][j] = j;
        for (int i = 1; i <= n; i++) {
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                int cost = (ca == cb) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[n][m];
    }

}
