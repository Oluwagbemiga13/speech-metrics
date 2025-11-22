package cz.oluwagbemiga.speech_metric.repository;

import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RecognitionSuiteRepository extends JpaRepository<RecognitionSuite, UUID> {
    // Fetch all suites belonging to a specific user (owner) by their UUID.
    List<RecognitionSuite> findAllByOwner_Id(UUID ownerId);
}
