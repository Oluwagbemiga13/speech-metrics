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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final AudioFileMapper audioFileMapper;

    public UserDTO saveUser(String username) {
        var userEntity = new User();
        userEntity.setUsername(username);
        var savedUser = userRepository.save(userEntity);
        return userMapper.toDto(savedUser);
    }

    @Transactional
    public void deleteUserByUsername(String username) {
        var success = userRepository.deleteByUsername(username);
        if (success == 0) {
            throw new UserNotExistException(username);
        }
    }

    @Transactional
    public UserDTO getUserByUsername(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotExistException(username));
        return userMapper.toDto(user);
    }


    @Transactional
    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAll();
        return userMapper.toDto(users);
    }

    @Transactional
    public UploadResponse addFileToUser(UUID userId, MultipartFile file) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotExistException("ID: " + userId));
        var audioFile = new AudioFile();
        try (var inputStream = file.getInputStream()) {
            audioFile.setData(inputStream.readAllBytes());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UploadFileException("Failed to upload file: " + file.getOriginalFilename());
        }
        audioFile.setFileName(file.getOriginalFilename());
        audioFile.setOwner(user);
        user.getAudioFiles().add(audioFile);

        user = userRepository.save(user);

        List<AudioFile> audioFiles = user.getAudioFiles();
        return new UploadResponse(audioFiles.get(audioFiles.size() - 1));
    }
}
