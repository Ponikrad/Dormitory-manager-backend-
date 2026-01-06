package com.dorm.manag.controller;

import com.dorm.manag.dto.ChangePasswordRequest;
import com.dorm.manag.dto.UpdateUserRequest;
import com.dorm.manag.entity.ProfileImage;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.ProfileImageService;
import com.dorm.manag.service.UserService;
import com.dorm.manag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final ProfileImageService profileImageService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfileImage(
            @RequestParam("image") MultipartFile file,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ProfileImage profileImage = profileImageService.uploadProfileImage(user, file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile image uploaded successfully");
            response.put("imageId", profileImage.getId());
            response.put("fileName", profileImage.getFileName());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error uploading profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to upload image", "message", e.getMessage()));
        }
    }

    @GetMapping("/image")
    public ResponseEntity<?> getMyProfileImage(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<ProfileImage> profileImage = profileImageService.getProfileImage(user);

            if (profileImage.isPresent()) {
                ProfileImage image = profileImage.get();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + image.getFileName() + "\"")
                        .body(image.getImageData());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No profile image found"));
            }
        } catch (Exception e) {
            log.error("Error fetching profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch image"));
        }
    }

    @GetMapping("/image/{userId}")
    public ResponseEntity<?> getProfileImageByUserId(@PathVariable Long userId) {
        try {
            Optional<ProfileImage> profileImage = profileImageService.getProfileImageByUserId(userId);

            if (profileImage.isPresent()) {
                ProfileImage image = profileImage.get();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.getFileType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + image.getFileName() + "\"")
                        .body(image.getImageData());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "No profile image found"));
            }
        } catch (Exception e) {
            log.error("Error fetching profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch image"));
        }
    }

    @DeleteMapping("/image")
    public ResponseEntity<?> deleteProfileImage(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            profileImageService.deleteProfileImage(user);

            return ResponseEntity.ok(Map.of("message", "Profile image deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting profile image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to delete image"));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateUserRequest updateRequest,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User updatedUser = userService.updateUserFields(user, updateRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", createUserResponse(updatedUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update profile", "message", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!request.passwordsMatch()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "New passwords do not match"));
            }

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Current password is incorrect"));
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (Exception e) {
            log.error("Error changing password: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to change password", "message", e.getMessage()));
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("roomNumber", user.getRoomNumber());
        userInfo.put("role", user.getRole().name());
        userInfo.put("active", user.isActive());
        userInfo.put("createdAt", user.getCreatedAt());
        return userInfo;
    }
}