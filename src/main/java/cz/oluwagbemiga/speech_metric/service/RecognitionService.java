package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.EngineOverviewDTO;
import cz.oluwagbemiga.speech_metric.dto.RecognitionResponse;
import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.engine.RecognitionRequest;
import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import cz.oluwagbemiga.speech_metric.repository.RecognitionResultRepository;
import cz.oluwagbemiga.speech_metric.repository.RecognitionSuiteRepository;
import cz.oluwagbemiga.speech_metric.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service coordinating speech recognition workflows.
 * <p>
 * Provides operations for executing recognition with a single selected engine or across all
 * registered engines, persisting resulting {@link cz.oluwagbemiga.speech_metric.entity.RecognitionResult}
 * instances (cascade via {@link cz.oluwagbemiga.speech_metric.entity.AudioFile}).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecognitionService {

    private final EngineService engineService;
    private final AudioFileService audioFileService;
    private final RecognitionSuiteRepository recognitionSuiteRepository;
    private final RecognitionResultRepository recognitionResultRepository;
    private final UserRepository userRepository;


    /**
     * Runs recognition for a single model selected by name.
     *
     * @param audioFileId source audio UUID
     * @param expected    expected transcript provided by user (used for accuracy metrics)
     * @param modelSelect engine key (see {@link EngineService#getEngineByName(String)})
     * @return response DTO based on newly persisted result
     */
    public RecognitionResponse recognizeAllEngines(
            UUID audioFileId,
            String expected,
            String modelSelect) {
        log.debug("Single-engine recognition request audioFileId={} model={} expectedChars={}", audioFileId, modelSelect, expected == null ? 0 : expected.length());

        AudioFile audioFile = audioFileService.getById(audioFileId);

        SpeechEngine engine = engineService.getEngineByName(modelSelect);

        engine.processAudio(new RecognitionRequest(audioFile, expected));

        // persist audio file with cascaded recognition result
        AudioFile saved = audioFileService.save(audioFile);
        RecognitionResult persisted = saved.getRecognitionResults().get(saved.getRecognitionResults().size() - 1);

        log.info("Recognition completed audioFileId={} model={} accuracy={}", audioFileId, persisted.getModelName(), persisted.getAccuracy());
        return mapToResponse(persisted);
    }

    /**
     * Executes recognition over all configured engines.
     *
     * @param audioFileId source audio UUID
     * @param expected    expected transcript for metric calculation
     * @return list of responses mapped from persisted results (one per engine)
     */
    public List<RecognitionResponse> recognizeAllEngines(
            UUID audioFileId,
            String expected) {
        log.debug("Multi-engine recognition request audioFileId={} expectedChars={}", audioFileId, expected == null ? 0 : expected.length());

        AudioFile audioFile = audioFileService.getById(audioFileId);

        List<RecognitionResult> results = new ArrayList<>();
        List<SpeechEngine> engines = engineService.getAllEngines();
        for (SpeechEngine engine : engines) {
            results.add(engine.processAudio(new RecognitionRequest(audioFile, expected)));
        }


        AudioFile saved = audioFileService.save(audioFile);

        long skipCount = saved.getRecognitionResults().size() == results.size() ? 0L :
                saved.getRecognitionResults().size() - results.size();

        var responses = saved.getRecognitionResults()
                .stream()
                .skip(skipCount)
                .map(this::mapToResponse)
                .toList();
        log.info("Multi-engine recognition finished audioFileId={} enginesProcessed={} responses={} ", audioFileId, engines.size(), responses.size());
        return responses;
    }

    /**
     * Executes recognition over all configured engines.
     *
     * @param audioFileId source audio UUID
     * @param expected    expected transcript for metric calculation
     * @return list of responses mapped from persisted results (one per engine)
     */
    public List<RecognitionResponse> recognizeAllEngines(
            UUID audioFileId,
            String expected,
            RecognitionSuite suite) {
        log.debug("Suite engine recognition request audioFileId={} suiteId={} expectedChars={}", audioFileId, suite.getId(), expected == null ? 0 : expected.length());

        AudioFile audioFile = audioFileService.getById(audioFileId);

        List<RecognitionResult> results = new ArrayList<>();
        List<SpeechEngine> engines = engineService.getAllEngines();
        for (SpeechEngine engine : engines) {
            RecognitionResult recognitionResult = engine.processAudio(new RecognitionRequest(audioFile, expected));
            recognitionResult.setRecognitionSuite(suite);
            results.add(recognitionResult);
        }


        AudioFile saved = audioFileService.save(audioFile);

        long skipCount = saved.getRecognitionResults().size() == results.size() ? 0L :
                saved.getRecognitionResults().size() - results.size();

        var responses = saved.getRecognitionResults()
                .stream()
                .skip(skipCount)
                .map(this::mapToResponse)
                .toList();
        log.info("Suite recognition finished audioFileId={} suiteId={} enginesProcessed={} responses={}", audioFileId, suite.getId(), engines.size(), responses.size());
        return responses;
    }

    /**
     * Placeholder for suite execution (batch processing across multiple audio files).
     *
     * @param expectedMap mapping of audio file id to expected transcript
     * @return list of recognition responses (currently null until implemented)
     */
    @Transactional
    public RecognitionSuiteDTO runSuite(Map<UUID, String> expectedMap, UUID ownerId) {
        log.debug("Run suite start audioFilesCount={}", expectedMap == null ? 0 : expectedMap.size());
        if (expectedMap == null || expectedMap.isEmpty()) {
            log.warn("Run suite called with empty expected map");
            return null;
        }

        RecognitionSuite recognitionSuite = new RecognitionSuite();
        recognitionSuite.setOwner(userRepository.findById(ownerId).orElseThrow());
        RecognitionSuite suite = recognitionSuiteRepository.save(recognitionSuite);
        List<RecognitionResponse> allResponses = new ArrayList<>();

        expectedMap.forEach((audioFileId, expected) -> {
            List<RecognitionResponse> responses = recognizeAllEngines(audioFileId, expected, suite);
            allResponses.addAll(responses);
        });

        log.info("Suite run complete suiteId={} totalResults={}", suite.getId(), allResponses.size());
        return new RecognitionSuiteDTO(suite.getId(), allResponses, suite.getOwner().getId(), suite.getCreatedAt());

    }


    private RecognitionResponse mapToResponse(RecognitionResult result) {
        var response = new RecognitionResponse(
                result.getId(),
                result.getModelName(),
                result.getRecognizedText(),
                result.getExpectedText(),
                result.getAccuracy(),
                result.getModelProcessingTimeMs()
        );
        log.trace("Mapped RecognitionResult id={} model={} accuracy={} modelMs={}", result.getId(), result.getModelName(), result.getAccuracy(), result.getModelProcessingTimeMs());
        return response;
    }

    /**
     * Returns all recognition results produced by the specified model.
     *
     * @param modelName engine identifier (case-insensitive)
     * @return list of recognition responses filtered by model name
     */
    @Transactional(readOnly = true)
    public List<RecognitionResponse> getResultsByModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            log.warn("getResultsByModel called with blank model name");
            return List.of();
        }

        log.debug("Fetching recognition results for modelName={}", modelName);
        return recognitionResultRepository.findAllByModelNameIgnoreCase(modelName)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns all recognition results for a specific user.
     *
     * @param userId the user's UUID
     * @return list of recognition responses for the user
     */
    @Transactional(readOnly = true)
    public List<RecognitionResponse> getResultsByUser(UUID userId) {
        if (userId == null) {
            log.warn("getResultsByUser called with null userId");
            return List.of();
        }

        log.debug("Fetching recognition results for userId={}", userId);
        return recognitionResultRepository.findAllByOwnerId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns an overview of a specific engine's performance.
     *
     * @param engineName the engine name to get overview for
     * @return EngineOverviewDTO with aggregated statistics
     */
    @Transactional(readOnly = true)
    public EngineOverviewDTO getEngineOverview(String engineName) {
        if (engineName == null || engineName.isBlank()) {
            log.warn("getEngineOverview called with blank engine name");
            return null;
        }

        log.debug("Building engine overview for engineName={}", engineName);
        List<RecognitionResult> results = recognitionResultRepository.findAllByModelNameIgnoreCase(engineName);

        if (results.isEmpty()) {
            log.info("No results found for engine={}", engineName);
            return new EngineOverviewDTO(engineName, List.of(), 0.0, 0, 0.0);
        }

        List<UUID> resultIds = results.stream()
                .map(RecognitionResult::getId)
                .toList();

        double averageAccuracy = results.stream()
                .mapToDouble(RecognitionResult::getAccuracy)
                .average()
                .orElse(0.0);

        double averageProcessingTime = results.stream()
                .mapToLong(RecognitionResult::getModelProcessingTimeMs)
                .average()
                .orElse(0.0);

        double totalAccuracyPercentage = averageAccuracy * 100;

        log.info("Engine overview built for engine={} totalResults={} avgAccuracy={}%",
                engineName, results.size(), String.format("%.2f", totalAccuracyPercentage));

        return new EngineOverviewDTO(
                engineName,
                resultIds,
                totalAccuracyPercentage,
                results.size(),
                averageProcessingTime
        );
    }

    /**
     * Returns overviews for all available engines.
     *
     * @return list of EngineOverviewDTO for all engines
     */
    @Transactional(readOnly = true)
    public List<EngineOverviewDTO> getAllEngineOverviews() {
        log.debug("Building overviews for all engines");
        List<String> engineNames = engineService.getAllEngineNames();

        return engineNames.stream()
                .map(this::getEngineOverview)
                .toList();
    }
}
