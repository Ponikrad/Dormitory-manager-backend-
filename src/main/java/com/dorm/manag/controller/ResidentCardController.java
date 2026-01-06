package com.dorm.manag.controller;

import com.dorm.manag.dto.ResidentCardDto;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.ResidentCardService;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/resident-cards")
@RequiredArgsConstructor
public class ResidentCardController {

    private final ResidentCardService residentCardService;
    private final UserService userService;

    @GetMapping("/my-card")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyCard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<ResidentCardDto> card = residentCardService.getCardByUser(user);

            if (card.isPresent()) {
                return ResponseEntity.ok(card.get());
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No resident card found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error fetching resident card", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch card");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> generateCard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ResidentCardDto card = residentCardService.generateCard(user);

            return ResponseEntity.ok(card);
        } catch (Exception e) {
            log.error("Error generating resident card", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate card");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/verify/{qrCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTIONIST')")
    public ResponseEntity<?> verifyQrCode(@PathVariable String qrCode) {
        try {
            Optional<ResidentCardDto> card = residentCardService.verifyQrCode(qrCode);

            if (card.isPresent()) {
                ResidentCardDto cardDto = card.get();
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("card", cardDto);
                response.put("message", "Card is valid");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("message", "Card not found or expired");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error verifying QR code", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Verification failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/deactivate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> deactivateMyCard(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            residentCardService.deactivateCard(user.getId());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Card deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deactivating card", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to deactivate card");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCardByUserId(@PathVariable Long userId) {
        try {
            Optional<ResidentCardDto> card = residentCardService.getCardByUserId(userId);

            if (card.isPresent()) {
                return ResponseEntity.ok(card.get());
            } else {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No card found for this user");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("Error fetching card by user ID", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch card");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}