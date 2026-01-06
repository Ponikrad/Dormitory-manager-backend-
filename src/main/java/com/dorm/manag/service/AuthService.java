package com.dorm.manag.service;

import com.dorm.manag.config.JwtTokenProvider;
import com.dorm.manag.dto.LoginRequest;
import com.dorm.manag.dto.RegisterRequest;
import com.dorm.manag.entity.Role;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate user and return JWT token
     */
    @Transactional
    public Map<String, Object> authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        try {

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtTokenProvider.generateToken(authentication);
            log.info("JWT token generated successfully for user: {}", loginRequest.getUsername());

            User user = userRepository.findByUsername(loginRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("user", buildUserResponse(user));
            response.put("message", "Login successful");

            log.info("User {} logged in successfully", user.getUsername());
            return response;

        } catch (AuthenticationException ex) {
            log.error("Authentication failed for user: {}: {}", loginRequest.getUsername(), ex.getMessage());
            throw new RuntimeException("Invalid username or password");
        } catch (Exception ex) {
            log.error("Error during authentication: {}", ex.getMessage(), ex);
            throw new RuntimeException("Authentication error: " + ex.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> registerUser(RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());

        try {
            if (userRepository.existsByUsername(registerRequest.getUsername())) {
                throw new RuntimeException("Username already exists");
            }

            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                throw new RuntimeException("Email already exists");
            }

            User newUser = new User();
            newUser.setUsername(registerRequest.getUsername());
            newUser.setEmail(registerRequest.getEmail());
            newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            newUser.setFirstName(registerRequest.getFirstName());
            newUser.setLastName(registerRequest.getLastName());
            newUser.setRole(Role.STUDENT); // Default role
            newUser.setActive(true);

            User savedUser = userRepository.save(newUser);
            log.info("User {} registered successfully", savedUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("user", buildUserResponse(savedUser));

            return response;

        } catch (Exception ex) {
            log.error("Error during registration: {}", ex.getMessage());
            throw new RuntimeException("Registration error: " + ex.getMessage());
        }
    }

    public Map<String, Object> getCurrentUserProfile(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new RuntimeException("Not authenticated");
            }

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return buildUserResponse(user);

        } catch (Exception ex) {
            log.error("Error retrieving profile: {}", ex.getMessage());
            throw new RuntimeException("Profile retrieval error: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put("username", user.getUsername());
        userResponse.put("email", user.getEmail());
        userResponse.put("firstName", user.getFirstName());
        userResponse.put("lastName", user.getLastName());
        userResponse.put("role", user.getRole().name());
        userResponse.put("active", user.isActive());
        userResponse.put("roomNumber", user.getRoomNumber());
        userResponse.put("phoneNumber", user.getPhoneNumber());
        userResponse.put("createdAt", user.getCreatedAt());
        return userResponse;
    }
}