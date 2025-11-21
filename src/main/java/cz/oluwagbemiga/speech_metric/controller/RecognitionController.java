package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.RecognitionResponse;
import cz.oluwagbemiga.speech_metric.engine.RecognitionRequest;
import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.service.AudioFileService;
import cz.oluwagbemiga.speech_metric.service.EngineService;
import cz.oluwagbemiga.speech_metric.service.RecognitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recognition")
@Tag(name = "Recognition API", description = "Run speech recognition on stored audio files")
public class RecognitionController {

    private final AudioFileService audioFileService;
    private final RecognitionService recognitionService;
    private final EngineService engineService;

    public RecognitionController(
            AudioFileService audioFileService,
            @Qualifier("voskLargeEngine") SpeechEngine voskLargeEngine,
            @Qualifier("voskSmallEngine") SpeechEngine voskSmallEngine,
            @Qualifier("whisperBaseEngine") SpeechEngine whisperBaseEngine,
            RecognitionService recognitionService,
            EngineService engineService) {
        this.audioFileService = audioFileService;
        this.recognitionService = recognitionService;
        this.engineService = engineService;
    }


    @PostMapping("/{audioFileId}")
    @Operation(summary = "Recognize speech in an audio file",
            description = "Provide audioFile UUID and expected text. Optional query param model=small|large|whisper selects engine.")
    @Transactional
    public ResponseEntity<RecognitionResponse> recognize(
            @PathVariable UUID audioFileId,
            @RequestParam String expected,
            @RequestParam(name = "model", defaultValue = "whisper-small-q8") String modelSelect) {

        AudioFile audioFile = audioFileService.getById(audioFileId);

        // choose engine by model parameter
        SpeechEngine engine = engineService.getEngineByName(modelSelect);

        // run recognition and attach result to the audio file (engine adds to collection)
        engine.processAudio(new RecognitionRequest(audioFile, expected));

        // persist audio file with cascaded recognition result
        AudioFile saved = audioFileService.save(audioFile);
        RecognitionResult persisted = saved.getRecognitionResults().get(saved.getRecognitionResults().size() - 1);

        RecognitionResponse response = new RecognitionResponse(
                persisted.getId(),
                persisted.getModelName(),
                persisted.getRecognizedText(),
                persisted.getExpectedText(),
                persisted.getAccuracy()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/all-engines/{audioFileId}")
    @Operation(summary = "Recognize speech in an audio file",
            description = "Provide audioFile UUID and expected text. Optional query param model=small|large|whisper selects engine.")
    @Transactional
    public ResponseEntity<List<RecognitionResponse>> recognizeByAllEngines(
            @PathVariable UUID audioFileId,
            @RequestParam String expected) {

        List<RecognitionResponse> responses = recognitionService.recognizeAllEngines(audioFileId, expected);

        return ResponseEntity.ok(responses);
    }

}
