package cz.oluwagbemiga.speech_metric.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Data transfer object representing an uploaded audio file.
 * <p>
 * Contains the unique identifier, original file name, raw WAV bytes and creation timestamp.
 * The raw byte array is included for endpoints that need to stream or process the audio directly.
 * </p>
 *
 * @param id        unique identifier of the audio file
 * @param fileName  original name supplied at upload time
 * @param data      raw audio bytes (typically a normalized WAV container)
 * @param createdAt timestamp when the file was persisted
 */
public record AudioFileDTO(
        UUID id,
        String fileName,
        byte[] data,
        LocalDateTime createdAt
) {
    @Override
    public String toString() {
        return "AudioFileDto{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", data=" + Arrays.toString(data) +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AudioFileDTO that = (AudioFileDTO) o;
        return Objects.equals(id, that.id) && Objects.deepEquals(data, that.data) && Objects.equals(fileName, that.fileName) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, Arrays.hashCode(data), createdAt);
    }
}
