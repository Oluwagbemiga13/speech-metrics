package cz.oluwagbemiga.speech_metric.controller;

import cz.oluwagbemiga.speech_metric.dto.AudioFileDTO;
import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.service.AudioFileService;
import cz.oluwagbemiga.speech_metric.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Audio File API", description = "CRUD operations for audio files")
public class AudioFileController {

    private final AudioFileService audioFileService;
    private final UserService userService;

    // Create: upload an audio file for a user. Returns updated UserDTO containing audioFileIds.
    @PostMapping(path = "/users/{userId}/audio-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload audio file for a user",
            description = "Uploads a single audio file for the given user and returns the updated user, including audioFileIds.")
    @ApiResponse(responseCode = "201", description = "File uploaded",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDTO.class)))
    public ResponseEntity<UserDTO> upload(@PathVariable UUID userId, @RequestPart("file") MultipartFile file) {
        UserDTO updated = userService.addFileToUser(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(updated);
    }

    // Read: download raw file bytes by ID
    @GetMapping("/audio-files/{id}")
    @Operation(summary = "Download audio file by ID")
    @ApiResponse(responseCode = "200", description = "File bytes returned")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        AudioFileDTO file = audioFileService.getDtoById(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", file.fileName());
        return new ResponseEntity<>(file.data(), headers, HttpStatus.OK);
    }

    // Get audio file DTO by ID
    @GetMapping("/audio-files/{id}/dto")
    @Operation(summary = "Get audio file DTO by ID",
            description = "Returns audio file metadata and content (base64-encoded) as JSON")
    @ApiResponse(responseCode = "200", description = "Audio file DTO returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AudioFileDTO.class)))
    public ResponseEntity<AudioFileDTO> getDto(@PathVariable UUID id) {
        return ResponseEntity.ok(audioFileService.getDtoById(id));
    }

    // List: all audio file IDs for a user
    @GetMapping("/users/{userId}/audio-files")
    @Operation(summary = "List a user's audio file IDs")
    @ApiResponse(responseCode = "200", description = "List of audio file IDs")
    public ResponseEntity<List<UUID>> listByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(audioFileService.getIdsByUserId(userId));
    }

    // List: all audio files for a user as DTOs
    @GetMapping("/users/{userId}/audio-files/dto")
    @Operation(summary = "List a user's audio files as DTOs",
            description = "Returns all audio files for the user, including metadata and base64-encoded content")
    @ApiResponse(responseCode = "200", description = "List of AudioFileDto returned",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AudioFileDTO.class)))
    public ResponseEntity<List<AudioFileDTO>> listDtosByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(audioFileService.getDtosByUserId(userId));
    }

    // Delete: delete audio file by ID
    @DeleteMapping("/audio-files/{id}")
    @Operation(summary = "Delete audio file by ID")
    @ApiResponse(responseCode = "204", description = "File deleted")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        audioFileService.deleteAudioFileById(id);
        return ResponseEntity.noContent().build();
    }

    // Update: rename a file by ID
    @PatchMapping("/audio-files/{id}")
    @Operation(summary = "Rename audio file by ID")
    @ApiResponse(responseCode = "204", description = "File renamed")
    public ResponseEntity<Void> rename(@PathVariable UUID id, @RequestParam String fileName) {
        audioFileService.rename(id, fileName);
        return ResponseEntity.noContent().build();
    }
}
