package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "Operations related to users")
public class UserController {

    private final UserService userService;


    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a user and returns the created user wrapped in a response object.")
    @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseEntity.class)))
    @ApiResponse(responseCode = "400", description = "Invalid request")

    public ResponseEntity<UserDTO> createUser(@Valid @RequestParam String username) {
        var saved = userService.saveUser(username);
        return ResponseEntity.status(201).body(saved);
    }

    // Get all users
    @GetMapping
    @Operation(summary = "Get all users", description = "Returns a list of all users.")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Get a user by username
    @GetMapping("/{username}")
    @Operation(summary = "Get a user by username", description = "Returns a single user by username.")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserDTO> getUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "Delete a user", description = "Deletes a user by username.")

    @ApiResponse(responseCode = "204", description = "User deleted")
    @ApiResponse(responseCode = "404", description = "User not found")

    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUserByUsername(username);
        return ResponseEntity.noContent().build();
    }

}
