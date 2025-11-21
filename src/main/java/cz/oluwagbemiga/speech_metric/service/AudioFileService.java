package cz.oluwagbemiga.speech_metric.service;

import cz.oluwagbemiga.speech_metric.dto.AudioFileDTO;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.exception.FileNotExist;
import cz.oluwagbemiga.speech_metric.exception.UploadFileException;
import cz.oluwagbemiga.speech_metric.mapper.AudioFileMapper;
import cz.oluwagbemiga.speech_metric.repository.AudioFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service providing CRUD and auxiliary operations for {@link cz.oluwagbemiga.speech_metric.entity.AudioFile} entities.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Persisting audio files while normalizing raw audio into a canonical WAV PCM s16le mono 16kHz format.</li>
 *   <li>Simple read operations returning entities or DTO projections.</li>
 *   <li>Utility accessors for file name, binary content and ID aggregation.</li>
 *   <li>Rename and delete operations with domain specific exception handling.</li>
 * </ul>
 * Normalization is delegated to {@link FfmpegService} and mapping to {@link cz.oluwagbemiga.speech_metric.mapper.AudioFileMapper}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;
    private final AudioFileMapper audioFileMapper;
    private final FfmpegService ffmpegService;

    /**
     * Deletes an audio file by its UUID.
     *
     * @param audioFileId identifier of the audio file
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist when the file does not exist
     */
    @Transactional
    public void deleteAudioFileById(UUID audioFileId) {
        log.debug("Attempting delete audioFileId={}", audioFileId);
        if (audioFileRepository.deleteByUUID(audioFileId) == 0) {
            throw new FileNotExist(audioFileId.toString());
        }
        log.info("Deleted audioFileId={}", audioFileId);
    }

    /**
     * Retrieves an {@link AudioFile} entity by id.
     *
     * @param id audio file UUID
     * @return found entity
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist if not present
     */
    @Transactional
    public AudioFile getById(UUID id) {
        log.trace("Fetch AudioFile entity id={}", id);
        return audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
    }

    /**
     * Retrieves an audio file projected as {@link AudioFileDTO}.
     *
     * @param id audio file UUID
     * @return DTO representation
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist if not present
     */
    @Transactional(readOnly = true)
    public AudioFileDTO getDtoById(UUID id) {
        log.trace("Fetch AudioFile DTO id={}", id);
        var entity = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        return audioFileMapper.toDto(entity);
    }

    /**
     * Returns list of audio file IDs owned by a user.
     *
     * @param userId user UUID
     * @return list of audio file UUIDs (never null)
     */
    @Transactional
    public List<UUID> getIdsByUserId(UUID userId) {
        log.debug("Listing audio file IDs for userId={}", userId);
        return audioFileRepository.findAllByOwner_Id(userId)
                .stream()
                .map(AudioFile::getId)
                .toList();
    }

    /**
     * Renames an audio file.
     *
     * @param id          audio file UUID
     * @param newFileName new file name (extension may be modified by later normalization routines)
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist if target file not found
     */
    public void rename(UUID id, String newFileName) {
        log.debug("Rename audioFile id={} newName={}", id, newFileName);
        var file = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        file.setFileName(newFileName);
        audioFileRepository.save(file);
        log.info("Renamed audioFile id={} newName={}", id, newFileName);
    }

    /**
     * Returns a defensive copy of the audio file bytes.
     *
     * @param id audio file UUID
     * @return byte array clone or null if data absent
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist if file not found
     */
    @Transactional(readOnly = true)
    public byte[] getContentById(UUID id) {
        log.trace("Fetching content for audioFile id={}", id);
        var file = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        return file.getData() == null ? null : file.getData().clone();
    }

    /**
     * Gets the stored file name.
     *
     * @param id audio file UUID
     * @return current file name
     * @throws cz.oluwagbemiga.speech_metric.exception.FileNotExist if not found
     */
    public String getFileName(UUID id) {
        log.trace("Fetching fileName for audioFile id={}", id);
        return audioFileRepository.findById(id)
                .map(AudioFile::getFileName)
                .orElseThrow(() -> new FileNotExist(id.toString()));
    }

    /**
     * Returns all audio files of a user mapped to DTOs.
     *
     * @param userId user UUID
     * @return list of DTOs (empty if none)
     */
    @Transactional(readOnly = true)
    public List<AudioFileDTO> getDtosByUserId(UUID userId) {
        log.debug("Listing audio files (DTO) for userId={}", userId);
        var files = audioFileRepository.findAllByOwner_Id(userId);
        return audioFileMapper.toDto(files);
    }

    /**
     * Persists an {@link AudioFile}. If raw data is present it will be normalized before persistence.
     *
     * @param audioFile entity to save (may contain raw bytes)
     * @return persisted entity with possibly transformed data/file name
     * @throws cz.oluwagbemiga.speech_metric.exception.UploadFileException when normalization fails
     */
    @Transactional
    public AudioFile save(AudioFile audioFile) {
        log.debug("Persist audioFile id={} fileName={} hasData={} dataBytes={} ", audioFile.getId(), audioFile.getFileName(), audioFile.getData() != null, audioFile.getData() == null ? 0 : audioFile.getData().length);
        // Normalize and store only converted audio (16kHz mono PCM s16le WAV)
        byte[] data = audioFile.getData();
        if (data != null && data.length > 0) {
            try {
                byte[] converted = ffmpegService.toWavPcmMono16k(data);
                audioFile.setData(converted);
                audioFile.setFileName(ffmpegService.withWavExtension(audioFile.getFileName()));
            } catch (IOException e) {
                log.error("Failed to normalize audio before saving", e);
                throw new UploadFileException("Audio normalization failed: ".concat(e.getMessage()));
            }
        }
        AudioFile savedFile = audioFileRepository.saveAndFlush(audioFile);
        log.info("Saved audioFile id={} fileName={} bytes={}", savedFile.getId(), savedFile.getFileName(), savedFile.getData() == null ? 0 : audioFile.getData().length);
        return savedFile;
    }
}
