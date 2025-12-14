package cz.oluwagbemiga.speech_metric.repository;

import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for querying persisted {@link RecognitionResult} entities.
 */
public interface RecognitionResultRepository extends JpaRepository<RecognitionResult, UUID> {

    List<RecognitionResult> findAllByModelNameIgnoreCase(String modelName);
}

