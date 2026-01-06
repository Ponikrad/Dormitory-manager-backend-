package com.dorm.manag.controller;

import com.dorm.manag.dto.ChatMessageDto;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.ChatService;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @GetMapping("/messages")
    public ResponseEntity<?> getMessages(Authentication authentication) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ChatMessageDto> messages = chatService.getRecentMessages(user);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving chat messages: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve messages"));
        }
    }

    @GetMapping("/messages/after")
    public ResponseEntity<?> getMessagesAfter(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime after) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<ChatMessageDto> messages = chatService.getMessagesAfter(after, user);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error retrieving chat messages after {}: {}", after, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve messages"));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            User user = userService.findByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Message content is required"));
            }

            ChatMessageDto message = chatService.sendMessage(user, content);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending chat message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to send message: " + e.getMessage()));
        }
    }
}
