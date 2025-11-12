package cz.oluwagbemiga.speech_metric.repository;

import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, UUID> {
}
