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

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;
    private final AudioFileMapper audioFileMapper;
    private final FfmpegService ffmpegService;

    @Transactional
    public void deleteAudioFileById(UUID audioFileId) {
        if (audioFileRepository.deleteByUUID(audioFileId) == 0) {
            throw new FileNotExist(audioFileId.toString());
        }
    }

    @Transactional
    public AudioFile getById(UUID id) {
        return audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
    }

    @Transactional(readOnly = true)
    public AudioFileDTO getDtoById(UUID id) {
        var entity = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        return audioFileMapper.toDto(entity);
    }

    @Transactional
    public List<UUID> getIdsByUserId(UUID userId) {
        return audioFileRepository.findAllByOwner_Id(userId)
                .stream()
                .map(AudioFile::getId)
                .toList();
    }


    public void rename(UUID id, String newFileName) {
        var file = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        file.setFileName(newFileName);
        audioFileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public byte[] getContentById(UUID id) {
        var file = audioFileRepository.findById(id)
                .orElseThrow(() -> new FileNotExist(id.toString()));
        return file.getData() == null ? null : file.getData().clone();
    }

    public String getFileName(UUID id) {
        return audioFileRepository.findById(id)
                .map(AudioFile::getFileName)
                .orElseThrow(() -> new FileNotExist(id.toString()));
    }

    @Transactional(readOnly = true)
    public List<AudioFileDTO> getDtosByUserId(UUID userId) {
        var files = audioFileRepository.findAllByOwner_Id(userId);
        return audioFileMapper.toDto(files);
    }

    @Transactional
    public AudioFile save(AudioFile audioFile) {
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
        return audioFileRepository.saveAndFlush(audioFile);
    }
}
