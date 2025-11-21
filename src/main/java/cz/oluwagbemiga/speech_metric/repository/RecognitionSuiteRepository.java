package cz.oluwagbemiga.speech_metric.repository;

import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecognitionSuiteRepository extends JpaRepository<RecognitionSuite, UUID> {
}
