package cz.oluwagbemiga.speech_metric.service;


import cz.oluwagbemiga.speech_metric.dto.AuthResponse;
import cz.oluwagbemiga.speech_metric.dto.RegisterRequest;
import cz.oluwagbemiga.speech_metric.entity.Role;
import cz.oluwagbemiga.speech_metric.entity.User;
import cz.oluwagbemiga.speech_metric.exception.UserLoginException;
import cz.oluwagbemiga.speech_metric.exception.UserRegistrationException;
import cz.oluwagbemiga.speech_metric.repository.UserRepository;
import cz.oluwagbemiga.speech_metric.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthService {

    @Value("${registration.secret-key}")
    private String registrationSecretKey;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse authenticate(String username, String password) {
        log.debug("Attempting to authenticate user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthService::getException);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw getException();
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        log.debug("Authentication successful for user: {}", user.getId());

        return new AuthResponse(token, username);
    }

    public AuthResponse registerUser(RegisterRequest request) {
        log.debug("Registering new user: {}", request.getUsername());

        if (!registrationSecretKey.equals(request.getSecretKey())) {
            log.warn("Invalid registration secret key for user: {}", request.getUsername());
            throw new UserRegistrationException("Invalid registration secret key");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);

        User savedUser;
        try {savedUser = userRepository.save(user);}
        catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage());
            throw new UserRegistrationException("Registration failed: " + e.getMessage());
        }
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRole());

        log.debug("Successfully registered user: {}", request.getUsername());
        return new AuthResponse(token, savedUser.getUsername());
    }

    public AuthResponse authenticateAdmin(String username, String password) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(AuthService::getException);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw getException();
        }

        if (!user.getRole().equals(Role.ADMIN)) {
            throw getException();
        }

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        log.debug("Authentication successful for admin: {}", user.getId());

        return new AuthResponse(token, username);
    }


    private static UserLoginException getException() {

        return new UserLoginException("Invalid username or password");
    }
}