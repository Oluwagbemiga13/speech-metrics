package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.EngineOverviewDTO;
import cz.oluwagbemiga.speech_metric.dto.RecognitionResponse;
import cz.oluwagbemiga.speech_metric.dto.RecognitionSuiteDTO;
import cz.oluwagbemiga.speech_metric.service.RecognitionService;
import cz.oluwagbemiga.speech_metric.service.RecognitionSuiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin controller for managing and analyzing recognition results.
 * <p>
 * Provides endpoints for administrators to retrieve results by user or engine,
 * and to get engine performance overviews.
 * </p>
 */
@RestController
@RequestMapping("/api/admin")
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Tag(name = "Admin API", description = "Administrative endpoints for recognition analysis")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RecognitionService recognitionService;
    private final RecognitionSuiteService recognitionSuiteService;

    public AdminController(RecognitionService recognitionService,
                           RecognitionSuiteService recognitionSuiteService) {
        this.recognitionService = recognitionService;
        this.recognitionSuiteService = recognitionSuiteService;
    }

    @GetMapping("/results/user/{userId}")
    @Operation(summary = "Get recognition results by user",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns all recognition results for a specific user.")
    public ResponseEntity<List<RecognitionResponse>> getResultsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(recognitionService.getResultsByUser(userId));
    }

    @GetMapping("/results/engine/{engineName}")
    @Operation(summary = "Get recognition results by engine",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns all recognition results processed by the specified engine.")
    public ResponseEntity<List<RecognitionResponse>> getResultsByEngine(@PathVariable String engineName) {
        return ResponseEntity.ok(recognitionService.getResultsByModel(engineName));
    }

    @GetMapping("/suites/user/{userId}")
    @Operation(summary = "Get recognition suites by user",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns all recognition suites owned by the specified user.")
    public ResponseEntity<List<RecognitionSuiteDTO>> getSuitesByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(recognitionSuiteService.getSuitesByUserId(userId));
    }

    @GetMapping("/engine-overview/{engineName}")
    @Operation(summary = "Get engine performance overview",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns an overview of the specified engine's performance including total accuracy percentage and all result IDs.")
    public ResponseEntity<EngineOverviewDTO> getEngineOverview(@PathVariable String engineName) {
        EngineOverviewDTO overview = recognitionService.getEngineOverview(engineName);
        if (overview == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/engine-overviews")
    @Operation(summary = "Get all engine performance overviews",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns performance overviews for all available engines.")
    public ResponseEntity<List<EngineOverviewDTO>> getAllEngineOverviews() {
        return ResponseEntity.ok(recognitionService.getAllEngineOverviews());
    }

    @GetMapping("/suites")
    @Operation(summary = "Get all recognition suites",
            security = @SecurityRequirement(name = "bearerAuth"),
            description = "Returns all recognition suites across all users.")
    public ResponseEntity<List<RecognitionSuiteDTO>> getAllSuites() {
        return ResponseEntity.ok(recognitionSuiteService.getAllSuites());
    }
}

