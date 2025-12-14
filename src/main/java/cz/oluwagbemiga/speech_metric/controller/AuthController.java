package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.AuthResponse;
import cz.oluwagbemiga.speech_metric.dto.LoginRequest;
import cz.oluwagbemiga.speech_metric.dto.RegisterRequest;
import cz.oluwagbemiga.speech_metric.exception.UserLoginException;
import cz.oluwagbemiga.speech_metric.exception.UserRegistrationException;
import cz.oluwagbemiga.speech_metric.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * REST controller exposing authentication endpoints.
 */
@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Authenticate user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authenticated successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.authenticate(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (UserLoginException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Register a new user and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Registration failed", content = @Content)
    })
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (UserRegistrationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @Operation(summary = "Authenticate admin and return JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or not admin", content = @Content)
    })
    @PostMapping(path = "/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.authenticateAdmin(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (UserLoginException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}

