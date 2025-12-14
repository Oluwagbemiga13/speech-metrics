package cz.oluwagbemiga.speech_metric.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthResponse(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Username is required")
        String username
) {
}