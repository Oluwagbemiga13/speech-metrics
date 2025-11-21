package cz.oluwagbemiga.speech_metric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * User data transfer object exposed by REST endpoints.
 * <p>
 * Carries a stable user identifier, validated username and the list of
 * audio file identifiers owned by the user. Validation annotations ensure
 * username constraints are enforced during request deserialization.
 * </p>
 *
 * @param id           unique identifier of the user
 * @param username     display or login name (3-50 chars, non blank)
 * @param audioFileIds collection of audio file ids associated with the user
 */
public record UserDTO(

        UUID id,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        List<UUID> audioFileIds
) {
}
