package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import cz.oluwagbemiga.speech_metric.repository.RecognitionSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecognitionSuiteService {

    private final RecognitionSuiteRepository recognitionSuiteRepository;

    public RecognitionSuite getById(UUID id) {
        return recognitionSuiteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RecognitionSuite not found with id: " + id));
    }

    public RecognitionSuiteDTO save(List<RecognitionResult> results) {
        RecognitionSuite suite = new RecognitionSuite();
        suite.setRecognitionResults(results);
        return new RecognitionSuiteDTO(recognitionSuiteRepository.save(suite));
    }

    public RecognitionSuiteDTO addResults(UUID suiteId, List<RecognitionResult> results) {
        RecognitionSuite suite = getById(suiteId);
        List<RecognitionResult> existingResults = suite.getRecognitionResults();
        existingResults.addAll(results);
        suite.setRecognitionResults(existingResults);
        return new RecognitionSuiteDTO(recognitionSuiteRepository.save(suite));
    }


}
