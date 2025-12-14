package cz.oluwagbemiga.speech_metric.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Base abstraction for speech recognition engines used in the application.
 * <p>
 * Implementations (e.g. {@link WhisperEngine}, {@link VoskEngine}) provide concrete
 * logic to transcribe audio and produce a {@link RecognitionResult}. This class
 * offers common utilities for WAV/PCM extraction, accuracy computation via
 * character error rate (CER) and helper normalization routines.
 * <p>
 * The expected input audio format for helper methods is a normalized WAV
 * container with PCM signed 16-bit little-endian, mono, 16 kHz samples.
 */
@Slf4j
@Getter
public abstract class SpeechEngine {

    protected static final float TARGET_SAMPLE_RATE = 16000.0f;
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final String pathToModel;
    protected final String rawModelName;
    protected final String name;

    /**
     * Constructs a speech engine with a path to the underlying model resources.
     * Derives a human friendly model name from the path.
     *
     * @param pathToModel absolute or canonical path to model file/directory
     */
    protected SpeechEngine(String pathToModel) {
        this.pathToModel = pathToModel;
        String normalizedPath = pathToModel.replace("\\", "/");
        this.rawModelName = normalizedPath
                .substring(normalizedPath.lastIndexOf("/") + 1)
                .replaceAll("\\.(bin|model)$", "");
        this.name = slugifyName(rawModelName);
        log.info("Initialized SpeechEngine name={} rawModelName={} modelPath={}", name, rawModelName, this.pathToModel);
    }

    /**
     * Converts a source string into a slug suitable for model naming.
     * Non-alphanumeric characters are replaced with hyphens, and
     * the result is lowercased. If the source is null or blank,
     * returns the default "model".
     *
     * @param source input string to slugify
     * @return slugified string
     */
    private String slugifyName(String source) {
        if (source == null || source.isBlank()) {
            return "model";
        }
        String slug = source
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "model" : slug;
    }

    /**
     * Performs recognition on the provided request and returns a populated result.
     * Implementations must handle transcription errors internally and still
     * return a non-null {@link RecognitionResult} instance.
     *
     * @param recognitionRequest request containing audio and expected transcript
     * @return recognition result populated with recognized text and accuracy metrics
     */
    public abstract RecognitionResult processAudio(RecognitionRequest recognitionRequest);

    /**
     * Extract raw PCM s16le mono 16 kHz byte samples from a standard WAV container.
     * Validates the header and locates the data chunk, handling potentially
     * malformed size fields produced by non-seekable writers.
     *
     * @param wav full WAV file bytes
     * @return raw PCM bytes (little-endian, 16-bit samples)
     * @throws IOException if header is invalid, format unexpected or data missing
     */
    protected byte[] extractPcmS16leMono16k(byte[] wav) throws IOException {
        if (wav == null || wav.length < 44) {
            log.warn("WAV too small or null length={}", wav == null ? 0 : wav.length);
            throw new IOException("Invalid or empty WAV data");
        }
        if (!equalsAscii(wav, 0, "RIFF") || !equalsAscii(wav, 8, "WAVE")) {
            log.warn("Invalid WAV header (missing RIFF/WAVE)");
            throw new IOException("Not a RIFF/WAVE file");
        }
        // Expect 'fmt ' chunk at 12 for typical PCM WAV written by ffmpeg; if not present, fail fast
        if (!equalsAscii(wav, 12, "fmt ")) {
            log.warn("Missing fmt chunk in WAV");
            throw new IOException("Missing fmt chunk");
        }
        ByteBuffer bb = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int subchunk1Size = bb.getInt(16);
        int audioFormat = bb.getShort(20) & 0xFFFF; // 1 = PCM
        int numChannels = bb.getShort(22) & 0xFFFF;
        int sampleRate = bb.getInt(24);
        int bitsPerSample = bb.getShort(34) & 0xFFFF;
        if (audioFormat != 1 || numChannels != 1 || sampleRate != (int) TARGET_SAMPLE_RATE || bitsPerSample != 16) {
            log.warn("Unexpected WAV format audioFormat={} channels={} sampleRate={} bitsPerSample={}", audioFormat, numChannels, sampleRate, bitsPerSample);
            throw new IOException("Unexpected WAV format; expected PCM s16le mono 16k");
        }
        int dataOffset = findDataChunk(wav, 12 + 8 + subchunk1Size);
        int headerSize = dataOffset + 8; // 'data' + size field
        if (dataOffset < 0 || headerSize > wav.length) {
            log.warn("Data chunk not found dataOffset={} headerSize={} wavLength={}", dataOffset, headerSize, wav.length);
            throw new IOException("WAV data chunk not found");
        }
        int reported = bb.getInt(dataOffset + 4);
        int remaining = wav.length - headerSize;
        // Clamp size when written to non-seekable stream (ffmpeg may set -1 or an invalid large value)
        int dataSize = reported;
        if (dataSize < 0 || dataSize > remaining) {
            log.debug("Adjusting dataSize reported={} remaining={} -> clamped={} ", reported, remaining, remaining);
            dataSize = remaining;
        }
        if (dataSize <= 0) {
            log.warn("Invalid computed dataSize={} (reported={})", dataSize, reported);
            throw new IOException("WAV data size invalid");
        }
        byte[] pcm = new byte[dataSize];
        System.arraycopy(wav, headerSize, pcm, 0, dataSize);
        log.debug("Extracted PCM samples bytes={} samples={}", dataSize, dataSize / 2);
        return pcm;
    }

    /**
     * Compare a sequence of bytes against an ASCII reference string.
     *
     * @param data   source byte array
     * @param offset start index within data
     * @param ascii  ASCII string to compare
     * @return true if bytes match exactly
     */
    private boolean equalsAscii(byte[] data, int offset, String ascii) {
        byte[] ref = ascii.getBytes(StandardCharsets.US_ASCII);
        if (offset + ref.length > data.length) return false;
        for (int i = 0; i < ref.length; i++) if (data[offset + i] != ref[i]) return false;
        return true;
    }

    /**
     * Locate the 'data' chunk within a WAV byte array, starting search at a specified index.
     * Performs conservative validation of chunk sizes and will fall back to
     * byte-wise scanning if an invalid size is encountered.
     *
     * @param data  WAV bytes
     * @param start initial offset to begin searching
     * @return offset of the 'data' chunk header or -1 if not found
     */
    private int findDataChunk(byte[] data, int start) {
        int i = start;
        if (i < 12) i = 12;
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        while (i + 8 <= data.length) {
            if (equalsAscii(data, i, "data")) return i;
            if (i + 8 > data.length) break;
            int size = bb.getInt(i + 4);
            // Guard against invalid sizes
            if (size < 0 || i + 8 + size > data.length) {
                // If invalid, try to continue byte-by-byte to find 'data'
                i++;
                continue;
            }
            i += 8 + size;
        }
        return -1;
    }

    /**
     * Compute accuracy using Character Error Rate: 1 - (editDistance / expected length).
     * Returns 0 when expected is null/blank. Result is clamped to [0,1].
     *
     * @param expected   ground-truth transcript
     * @param recognized recognized transcript
     * @return CER-based accuracy value
     */
    protected double computeAccuracy(String expected, String recognized) {
        if (expected == null) {
            log.debug("Accuracy short-circuit: expected is null");
            return 0.0d;
        }
        String exp = normalizeForCer(expected);
        if (exp.isBlank()) {
            log.debug("Accuracy short-circuit: expected blank after normalization original='{}'", expected);
            return 0.0d;
        }
        // Remove any occurrences of the placeholder token (case-insensitive) inside the recognized text
        if (recognized != null && recognized.toLowerCase().contains("[blank_audio]")) {
            String original = recognized;
            recognized = recognized.replaceAll("(?i)\\[blank_audio\\]", " "); // replace with space to preserve word boundaries
            // collapse multiple spaces after removal
            recognized = recognized.replaceAll("\\s+", " ").trim();
            log.debug("Accuracy: stripped '[BLANK_AUDIO]' placeholders originalLen={} newLen={}", original.length(), recognized.length());
        }
        String rec = normalizeForCer(recognized == null ? "" : recognized);
        int distance = levenshteinChars(exp, rec);
        double acc = Math.max(0d, 1d - (double) distance / exp.length());
        log.debug("Computed accuracy distance={} expectedLen={} recognizedLen={} accuracy={}", distance, exp.length(), rec.length(), acc);
        return acc;
    }

    /**
     * Normalize a string for CER computation: lowercase, remove punctuation symbols
     * (question mark, period, comma, exclamation) and collapse whitespace.
     *
     * @param s input string
     * @return normalized string
     */
    protected String normalizeForCer(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replaceAll("[?.,!]", "") // remove ? . , !
                .trim()
                .replaceAll("\\s+", " ");
    }

    /**
     * Compute the Levenshtein edit distance between two character sequences.
     *
     * @param a first string
     * @param b second string
     * @return edit distance
     */
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
        int result = dp[n][m];
        log.trace("Levenshtein computed n={} m={} distance={}", n, m, result);
        return result;
    }

}
