package cz.oluwagbemiga.speech_metric.service;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.exception.FileNotExist;
import cz.oluwagbemiga.speech_metric.repository.AudioFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;


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
}
