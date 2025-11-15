package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.RecognitionResponse;
import cz.oluwagbemiga.speech_metric.engine.RecognitionRequest;
import cz.oluwagbemiga.speech_metric.engine.SpeechEngine;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.RecognitionResult;
import cz.oluwagbemiga.speech_metric.service.AudioFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/recognition")
@Tag(name = "Recognition API", description = "Run speech recognition on stored audio files")
public class RecognitionController {

    private final AudioFileService audioFileService;
    private final SpeechEngine voskLargeEngine;
    private final SpeechEngine voskSmallEngine;


    public RecognitionController(
            AudioFileService audioFileService,
            @Qualifier("voskLargeEngine") SpeechEngine voskLargeEngine,
            @Qualifier("voskSmallEngine") SpeechEngine voskSmallEngine) {
        this.audioFileService = audioFileService;
        this.voskLargeEngine = voskLargeEngine;
        this.voskSmallEngine = voskSmallEngine;
    }


    @PostMapping("/{audioFileId}")
    @Operation(summary = "Recognize speech in an audio file",
            description = "Provide audioFile UUID and expected text. Optional query param model=small|large selects engine.")
    public ResponseEntity<RecognitionResponse> recognize(
            @PathVariable UUID audioFileId,
            @RequestParam String expected,
            @RequestParam(name = "model", defaultValue = "large") String modelSelect) {

        AudioFile audioFile = audioFileService.getById(audioFileId);

        // choose engine by model parameter (default large)
        SpeechEngine engine = "small".equalsIgnoreCase(modelSelect) ? voskSmallEngine : voskLargeEngine;

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
}
