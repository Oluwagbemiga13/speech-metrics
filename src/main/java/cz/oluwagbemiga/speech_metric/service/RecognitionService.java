package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.RecognitionResponse;
import cz.oluwagbemiga.speech_metric.engine.RecognitionRequest;
import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.entity.RecognitionSuite;
import cz.oluwagbemiga.speech_metric.repository.RecognitionSuiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecognitionService {

    private final EngineService engineService;
    private final AudioFileService audioFileService;
    private final RecognitionSuiteRepository recognitionSuiteRepository;


    public RecognitionResponse recognizeAllEngines(
            UUID audioFileId,
            String expected,
            String modelSelect) {

        AudioFile audioFile = audioFileService.getById(audioFileId);

        SpeechEngine engine = engineService.getEngineByName(modelSelect);

        engine.processAudio(new RecognitionRequest(audioFile, expected));

        // persist audio file with cascaded recognition result
        AudioFile saved = audioFileService.save(audioFile);
        RecognitionResult persisted = saved.getRecognitionResults().get(saved.getRecognitionResults().size() - 1);

        return mapToResponse(persisted);
    }

    public List<RecognitionResponse> recognizeAllEngines(
            UUID audioFileId,
            String expected) {

        AudioFile audioFile = audioFileService.getById(audioFileId);

        RecognitionSuite recognitionSuite = new RecognitionSuite();


        List<RecognitionResult> results = new ArrayList<>();
        List<SpeechEngine> engines = engineService.getAllEngines();
        for (SpeechEngine engine : engines) {
            results.add(engine.processAudio(new RecognitionRequest(audioFile, expected)));
        }


        AudioFile saved = audioFileService.save(audioFile);

        long skipCount = saved.getRecognitionResults().size() == results.size() ? 0L :
                saved.getRecognitionResults().size() - results.size();

        return saved.getRecognitionResults()
                .stream()
                .skip(skipCount)
                .map(this::mapToResponse)
                .toList();
    }

    //TODO implement
    public List<RecognitionResponse> runSuite(Map<UUID, String> expectedMap) {

        return null;
    }


    private RecognitionResponse mapToResponse(RecognitionResult result) {
        return new RecognitionResponse(
                result.getId(),
                result.getModelName(),
                result.getRecognizedText(),
                result.getExpectedText(),
                result.getAccuracy()
        );

    }


}
