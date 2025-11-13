package cz.oluwagbemiga.speech_metric.dto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record AudioFileDto(
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
        AudioFileDto that = (AudioFileDto) o;
        return Objects.equals(id, that.id) && Objects.deepEquals(data, that.data) && Objects.equals(fileName, that.fileName) && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, Arrays.hashCode(data), createdAt);
    }
}
