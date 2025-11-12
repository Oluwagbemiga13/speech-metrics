package cz.oluwagbemiga.speech_metric.service;

import cz.oluwagbemiga.speech_metric.dto.UserDTO;
import cz.oluwagbemiga.speech_metric.entity.User;
import cz.oluwagbemiga.speech_metric.exception.UserNotExistException;
import cz.oluwagbemiga.speech_metric.mapper.UserMapper;
import cz.oluwagbemiga.speech_metric.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

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

    public UserDTO getUserByUsername(String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotExistException(username));
        return userMapper.toDto(user);
    }

    // Fetch all users
    public List<UserDTO> getAllUsers() {
        var users = userRepository.findAll();
        return userMapper.toDto(users);
    }
}
