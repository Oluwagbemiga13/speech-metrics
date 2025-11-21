package cz.oluwagbemiga.speech_metric.service;

import cz.oluwagbemiga.speech_metric.dto.UploadResponse;
import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.entity.AudioFile;
import cz.oluwagbemiga.speech_metric.entity.User;
import cz.oluwagbemiga.speech_metric.exception.UploadFileException;
import cz.oluwagbemiga.speech_metric.exception.UserNotExistException;
import cz.oluwagbemiga.speech_metric.mapper.AudioFileMapper;
import cz.oluwagbemiga.speech_metric.mapper.UserMapper;
import cz.oluwagbemiga.speech_metric.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service managing {@link cz.oluwagbemiga.speech_metric.entity.User} lifecycle and user-owned audio uploads.
 * <p>
 * Provides CRUD operations for users and supports uploading audio which is normalized before persistence.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final AudioFileMapper audioFileMapper;

    private final FfmpegService ffmpegService;

    /**
     * Creates and persists a new user with given username.
     *
     * @param username unique username
     * @return persisted user DTO
     */
    public UserDTO saveUser(String username) {
        var userEntity = new User();
        userEntity.setUsername(username);
        var savedUser = userRepository.save(userEntity);
        return userMapper.toDto(savedUser);
    }

    /**
     * Deletes user by username.
     *
     * @param username username to delete
     * @throws cz.oluwagbemiga.speech_metric.exception.UserNotExistException if user absent
     */
    @Transactional
    public void deleteUserByUsername(String username) {
        var success = userRepository.deleteByUsername(username);
        if (success == 0) {
            throw new UserNotExistException(username);
        }
    }

    /**
     * Fetch user by username.
     *
     * @param username username
     * @return user DTO
     * @throws cz.oluwagbemiga.speech_metric.exception.UserNotExistException if user not found
     */
    @Transactional
    public UserDTO getUserByUsername(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotExistException(username));
        return userMapper.toDto(user);
    }

    /**
     * Returns all users mapped to DTOs.
     *
     * @return list of user DTOs
     */
    @Transactional
    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAll();
        return userMapper.toDto(users);
    }

    /**
     * Adds an uploaded audio file to a user. Audio is normalized (WAV PCM s16le mono 16kHz) before persistence.
     *
     * @param userId id of owner user
     * @param file   multipart upload
     * @return upload response containing stored audio metadata
     * @throws cz.oluwagbemiga.speech_metric.exception.UserNotExistException if user not found
     * @throws cz.oluwagbemiga.speech_metric.exception.UploadFileException   on IO/processing errors
     */
    @Transactional
    public UploadResponse addFileToUser(UUID userId, MultipartFile file) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotExistException("ID: " + userId));
        var audioFile = new AudioFile();
        try (var inputStream = file.getInputStream()) {
            byte[] original = inputStream.readAllBytes();
            byte[] normalized = ffmpegService.toWavPcmMono16k(original);
            audioFile.setData(normalized);
            audioFile.setFileName(ffmpegService.withWavExtension(file.getOriginalFilename()));
        } catch (IOException e) {
            log.error("Failed to read/convert uploaded audio", e);
            throw new UploadFileException("Failed to upload file: " + file.getOriginalFilename());
        } catch (Exception e) {
            log.error("Unexpected error during upload", e);
            throw new UploadFileException("Failed to upload file: " + file.getOriginalFilename());
        }
        audioFile.setOwner(user);
        user.getAudioFiles().add(audioFile);

        user = userRepository.save(user);

        List<AudioFile> audioFiles = user.getAudioFiles();
        return new UploadResponse(audioFiles.get(audioFiles.size() - 1));
    }
}
