package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.service.RecognitionSuiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/recognition-suites")
@Tag(name = "Recognition Suite API", description = "Retrieve recognition suites and their aggregated results")
public class RecognitionSuiteController {

    private final RecognitionSuiteService recognitionSuiteService;

    public RecognitionSuiteController(RecognitionSuiteService recognitionSuiteService) {
        this.recognitionSuiteService = recognitionSuiteService;
    }

    @GetMapping
    @Operation(summary = "List all recognition suites", description = "Returns all suites across all users.")
    public ResponseEntity<List<RecognitionSuiteDTO>> getAllSuites() {
        return ResponseEntity.ok(recognitionSuiteService.getAllSuites());
    }

    @GetMapping("/{suiteId}")
    @Operation(summary = "Get a recognition suite by id", description = "Returns a single suite including its recognition results.")
    public ResponseEntity<RecognitionSuiteDTO> getSuite(@PathVariable UUID suiteId) {
        return ResponseEntity.ok(recognitionSuiteService.getSuiteDTOById(suiteId));
    }

    @GetMapping("/owner/{ownerId}")
    @Operation(summary = "List suites by owner", description = "Returns all suites belonging to the specified user UUID.")
    public ResponseEntity<List<RecognitionSuiteDTO>> getSuitesByOwner(@PathVariable UUID ownerId) {
        return ResponseEntity.ok(recognitionSuiteService.getSuitesByOwner(ownerId));
    }
}

