package cz.oluwagbemiga.speech_metric.dto;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response returned after successfully uploading an audio file.
 * <p>
 * Provides the persisted audio file identifier, original file name and creation timestamp
 * so the client can reference the file in subsequent recognition requests.
 * </p>
 *
 * @param audioFileId unique identifier of the stored audio file
 * @param fileName    original file name provided by the client
 * @param createdAt   timestamp when the file was stored
 */
public record UploadResponse(
        UUID audioFileId,
        String fileName,
        LocalDateTime createdAt
) {
    /**
     * Explicit canonical constructor (retained for clarity and potential validation).
     */
    public UploadResponse(UUID audioFileId, String fileName, LocalDateTime createdAt) {
        this.audioFileId = audioFileId;
        this.fileName = fileName;
        this.createdAt = createdAt;
    }

    /**
     * Convenience constructor mapping from an {@link AudioFile} entity instance.
     *
     * @param audioFile source entity
     */
    public UploadResponse(AudioFile audioFile) {
        this(
                audioFile.getId(),
                audioFile.getFileName(),
                audioFile.getCreatedAt()
        );
    }
}
