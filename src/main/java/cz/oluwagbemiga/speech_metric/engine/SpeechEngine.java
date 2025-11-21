package cz.oluwagbemiga.speech_metric.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;


@Slf4j
public abstract class SpeechEngine {

    protected static final float TARGET_SAMPLE_RATE = 16000.0f;
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

    // Extract raw PCM s16le mono 16kHz bytes from a normalized WAV container.
    // Assumes files were normalized at upload time.
    protected byte[] extractPcmS16leMono16k(byte[] wav) throws IOException {
        if (wav == null || wav.length < 44) throw new IOException("Invalid or empty WAV data");
        if (!equalsAscii(wav, 0, "RIFF") || !equalsAscii(wav, 8, "WAVE")) {
            throw new IOException("Not a RIFF/WAVE file");
        }
        // Expect 'fmt ' chunk at 12 for typical PCM WAV written by ffmpeg; if not present, fail fast
        if (!equalsAscii(wav, 12, "fmt ")) throw new IOException("Missing fmt chunk");
        ByteBuffer bb = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        int subchunk1Size = bb.getInt(16);
        int audioFormat = bb.getShort(20) & 0xFFFF; // 1 = PCM
        int numChannels = bb.getShort(22) & 0xFFFF;
        int sampleRate = bb.getInt(24);
        int bitsPerSample = bb.getShort(34) & 0xFFFF;
        if (audioFormat != 1 || numChannels != 1 || sampleRate != (int) TARGET_SAMPLE_RATE || bitsPerSample != 16) {
            throw new IOException("Unexpected WAV format; expected PCM s16le mono 16k");
        }
        int dataOffset = findDataChunk(wav, 12 + 8 + subchunk1Size);
        int headerSize = dataOffset + 8; // 'data' + size field
        if (dataOffset < 0 || headerSize > wav.length) throw new IOException("WAV data chunk not found");
        int reported = bb.getInt(dataOffset + 4);
        int remaining = wav.length - headerSize;
        // Clamp size when written to non-seekable stream (ffmpeg may set -1 or an invalid large value)
        int dataSize = reported;
        if (dataSize < 0 || dataSize > remaining) {
            dataSize = remaining;
        }
        if (dataSize <= 0) throw new IOException("WAV data size invalid");
        byte[] pcm = new byte[dataSize];
        System.arraycopy(wav, headerSize, pcm, 0, dataSize);
        return pcm;
    }

    private boolean equalsAscii(byte[] data, int offset, String ascii) {
        byte[] ref = ascii.getBytes(StandardCharsets.US_ASCII);
        if (offset + ref.length > data.length) return false;
        for (int i = 0; i < ref.length; i++) if (data[offset + i] != ref[i]) return false;
        return true;
    }

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
                .replaceAll("[?.,!]", "") // remove ? . , !
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
