package cz.oluwagbemiga.speech_metric.dto;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;

import java.time.LocalDateTime;
import java.util.UUID;

public record UploadResponse(
        UUID audioFileId,
        String fileName,
        LocalDateTime createdAt
) {
    public UploadResponse(UUID audioFileId, String fileName, LocalDateTime createdAt) {
        this.audioFileId = audioFileId;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }

    public UploadResponse(AudioFile audioFile) {
        this(
                audioFile.getId(),
                audioFile.getFileName(),
                audioFile.getCreatedAt()
        );
    }
}
