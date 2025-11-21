package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import cz.oluwagbemiga.speech_metric.repository.RecognitionSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service handling persistence and aggregation of {@link cz.oluwagbemiga.speech_metric.entity.RecognitionSuite}.
 * A suite groups multiple {@link cz.oluwagbemiga.speech_metric.entity.RecognitionResult} instances produced
 * across engines or batch runs for later comparative analysis.
 */
@Service
@RequiredArgsConstructor
public class RecognitionSuiteService {

    private final RecognitionSuiteRepository recognitionSuiteRepository;

    /**
     * Get suite by id.
     *
     * @param id suite UUID
     * @return found suite
     * @throws RuntimeException if not found (consider custom exception later)
     */
    public RecognitionSuite getById(UUID id) {
        return recognitionSuiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecognitionSuite not found with id: " + id));
    }

    /**
     * Create and persist a new suite populated with initial results.
     *
     * @param results recognition results to associate
     * @return DTO representation of persisted suite
     */
    public RecognitionSuiteDTO save(List<RecognitionResult> results) {
        RecognitionSuite suite = new RecognitionSuite();
        suite.setRecognitionResults(results);
        return new RecognitionSuiteDTO(recognitionSuiteRepository.save(suite));
    }

    /**
     * Append results to existing suite, then persist changes.
     *
     * @param suiteId target suite UUID
     * @param results additional results to merge
     * @return updated suite DTO
     */
    public RecognitionSuiteDTO addResults(UUID suiteId, List<RecognitionResult> results) {
        RecognitionSuite suite = getById(suiteId);
        List<RecognitionResult> existingResults = suite.getRecognitionResults();
        existingResults.addAll(results);
        suite.setRecognitionResults(existingResults);
        return new RecognitionSuiteDTO(recognitionSuiteRepository.save(suite));
    }


}
