package cz.oluwagbemiga.speech_metric.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Service for normalizing audio bytes into a recognition-friendly format.
 * Default target: WAV container, PCM s16le, mono, 16kHz.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FfmpegService {

    private static final String FFMPEG_CMD = System.getenv("FFMPEG_PATH") != null ? System.getenv("FFMPEG_PATH") : "ffmpeg";

    /**
     * Convert arbitrary audio bytes into WAV PCM s16le mono 16kHz. If input is already in this
     * format, returns the original bytes to avoid extra work.
     */
    public byte[] toWavPcmMono16k(byte[] input) throws IOException {
        if (input == null || input.length == 0) return input;
        try {
            if (isWavPcmMono16k(input)) {
                return input; // already normalized
            }
        } catch (Exception e) {
            // If header parsing fails, fall back to ffmpeg conversion
            log.debug("WAV header parse failed, falling back to ffmpeg", e);
        }
        return ffmpegTranscodeToWavPcmMono16k(input);
    }

    /**
     * Ensure filename ends with .wav to reflect normalized format.
     */
    public String withWavExtension(String originalName) {
        if (originalName == null || originalName.isBlank()) return "audio.wav";
        String lower = originalName.toLowerCase();
        if (lower.endsWith(".wav")) return originalName;
        int dot = originalName.lastIndexOf('.');
        String base = dot > 0 ? originalName.substring(0, dot) : originalName;
        return base + ".wav";
    }

    private byte[] ffmpegTranscodeToWavPcmMono16k(byte[] input) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_CMD,
                "-hide_banner",
                "-loglevel", "error",
                "-i", "pipe:0",
                "-f", "wav",
                "-acodec", "pcm_s16le",
                "-ac", "1",
                "-ar", "16000",
                "pipe:1"
        );
        Process proc;
        try {
            proc = pb.start();
        } catch (IOException ioe) {
            throw new IOException("Failed to start ffmpeg for audio normalization", ioe);
        }
        try (OutputStream os = proc.getOutputStream()) {
            os.write(input);
        }
        byte[] out = proc.getInputStream().readAllBytes();
        try {
            int exit = proc.waitFor();
            if (exit != 0 || out.length == 0) {
                String err = new String(proc.getErrorStream().readAllBytes());
                log.error("ffmpeg normalization failed (exit {}): {}", exit, err);
                throw new IOException("ffmpeg normalization failed, exit=" + exit);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during ffmpeg normalization", ie);
        }
        return out;
    }

    // Lightweight WAV header parser to detect PCM s16le mono 16kHz
    private boolean isWavPcmMono16k(byte[] data) {
        if (data.length < 44) return false;
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        // ChunkID "RIFF"
        if (!equalsAscii(data, 0, "RIFF")) return false;
        // Format "WAVE"
        if (!equalsAscii(data, 8, "WAVE")) return false;
        // Subchunk1ID "fmt " at offset 12
        int fmtIndex = 12;
        if (!equalsAscii(data, fmtIndex, "fmt ")) return false;
        int subchunk1Size = bb.getInt(16); // usually 16 for PCM
        int audioFormat = bb.getShort(20) & 0xFFFF; // 1 = PCM
        int numChannels = bb.getShort(22) & 0xFFFF;
        int sampleRate = bb.getInt(24);
        int bitsPerSample = bb.getShort(34) & 0xFFFF;
        if (audioFormat != 1) return false;
        if (numChannels != 1) return false;
        if (sampleRate != 16000) return false;
        if (bitsPerSample != 16) return false;
        // Optionally verify data subchunk exists
        int dataIdx = findDataChunk(data, 12 + 8 + subchunk1Size);
        return dataIdx > 0;
    }

    private int findDataChunk(byte[] data, int start) {
        int i = start;
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        while (i + 8 <= data.length) {
            if (equalsAscii(data, i, "data")) {
                return i;
            }
            // read chunk size and skip
            if (i + 8 > data.length) break;
            int size = bb.getInt(i + 4);
            i += 8 + Math.max(0, size);
        }
        return -1;
    }

    private boolean equalsAscii(byte[] data, int offset, String ascii) {
        byte[] ref = ascii.getBytes(StandardCharsets.US_ASCII);
        if (offset + ref.length > data.length) return false;
        for (int i = 0; i < ref.length; i++) if (data[offset + i] != ref[i]) return false;
        return true;
    }
}
