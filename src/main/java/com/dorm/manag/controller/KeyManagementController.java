package com.dorm.manag.controller;

import com.dorm.manag.dto.KeyAssignmentDto;
import com.dorm.manag.dto.KeyDto;
import com.dorm.manag.entity.DormitoryKey;
import com.dorm.manag.entity.KeyAssignment;
import com.dorm.manag.entity.KeyStatus;
import com.dorm.manag.entity.KeyType;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.KeyRepository;
import com.dorm.manag.service.KeyManagementService;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class KeyManagementController {

    private final KeyManagementService keyManagementService;
    private final UserService userService;
    private final KeyRepository keyRepository;

    // Pobierz wszystkie klucze

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAllKeys() {
        try {
            List<KeyDto> keys = keyManagementService.getAllKeys();
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Error retrieving keys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve keys"));
        }
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAvailableKeys() {
        try {
            List<KeyDto> keys = keyManagementService.getAvailableKeys();
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Error retrieving available keys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve keys"));
        }
    }

    @GetMapping("/type/{keyType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getKeysByType(@PathVariable String keyType) {
        try {
            KeyType type = KeyType.valueOf(keyType.toUpperCase());
            List<KeyDto> keys = keyManagementService.getKeysByType(type);
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Error retrieving keys by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid key type"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getKeyById(@PathVariable Long id) {
        try {
            KeyDto key = keyManagementService.getKeyById(id)
                    .orElseThrow(() -> new RuntimeException("Key not found"));
            return ResponseEntity.ok(key);
        } catch (Exception e) {
            log.error("Error retrieving key {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Key not found"));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyKeys(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<KeyDto> keys = keyManagementService.getUserKeys(user);
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Error retrieving user keys: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve keys"));
        }
    }

    @PostMapping("/{keyId}/issue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> issueKey(
            @PathVariable Long keyId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        try {
            String issuedByUsername = authentication.getName();
            User issuedBy = userService.findByUsername(issuedByUsername)
                    .orElseThrow(() -> new RuntimeException("Issuer not found"));

            Long userId = Long.valueOf(request.get("userId").toString());
            String assignmentType = request.getOrDefault("assignmentType", "PERMANENT").toString();
            String notes = request.getOrDefault("notes", "").toString();

            KeyAssignment.AssignmentType type = KeyAssignment.AssignmentType.valueOf(assignmentType.toUpperCase());

            KeyAssignmentDto assignment = keyManagementService.issueKey(
                    keyId, userId, issuedBy, type, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Key issued successfully");
            response.put("assignment", assignment);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error issuing key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to issue key", "message", e.getMessage()));
        }
    }

    @PostMapping("/assignments/{assignmentId}/return")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> returnKey(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String returnedToUsername = authentication.getName();
            User returnedTo = userService.findByUsername(returnedToUsername)
                    .orElseThrow(() -> new RuntimeException("Receptionist not found"));

            String condition = request.getOrDefault("condition", "Good");
            String notes = request.getOrDefault("notes", "");

            KeyAssignmentDto assignment = keyManagementService.returnKey(
                    assignmentId, returnedTo, condition, notes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Key returned successfully");
            response.put("assignment", assignment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error returning key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to return key", "message", e.getMessage()));
        }
    }

    @PostMapping("/assignments/{assignmentId}/report-lost")
    public ResponseEntity<?> reportKeyLost(
            @PathVariable Long assignmentId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            keyManagementService.reportKeyLost(assignmentId, user);

            return ResponseEntity.ok(Map.of("message", "Key reported as lost"));
        } catch (Exception e) {
            log.error("Error reporting lost key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to report lost key", "message", e.getMessage()));
        }
    }

    @PostMapping("/{keyId}/report-damaged")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> reportKeyDamaged(
            @PathVariable Long keyId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String damageDescription = request.getOrDefault("description", "Damaged");

            keyManagementService.reportKeyDamaged(keyId, damageDescription, user);

            return ResponseEntity.ok(Map.of("message", "Key reported as damaged"));
        } catch (Exception e) {
            log.error("Error reporting damaged key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to report damaged key", "message", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createKey(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String keyCode = request.get("keyCode");
            String description = request.get("description");
            KeyType keyType = KeyType.valueOf(request.get("keyType").toUpperCase());
            String roomNumber = request.getOrDefault("roomNumber", null);

            KeyDto key = keyManagementService.createKey(keyCode, description, keyType, roomNumber, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Key created successfully");
            response.put("key", key);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create key", "message", e.getMessage()));
        }
    }

    @PutMapping("/{keyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateKey(
            @PathVariable Long keyId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            // User user = userService.findByUsername(username)
            //         .orElseThrow(() -> new RuntimeException("User not found"));

            DormitoryKey key = keyRepository.findById(keyId)
                    .orElseThrow(() -> new RuntimeException("Key not found"));

            // Aktualizuj pola
            if (request.containsKey("description")) {
                key.setDescription(request.get("description"));
            }
            if (request.containsKey("keyType")) {
                key.setKeyType(KeyType.valueOf(request.get("keyType").toUpperCase()));
            }
            if (request.containsKey("roomNumber")) {
                key.setRoomNumber(request.get("roomNumber"));
                if (request.get("roomNumber") != null && !request.get("roomNumber").isEmpty()) {
                    key.setFloorNumber(Integer.parseInt(request.get("roomNumber").substring(0, 1)));
                }
            }

            if (request.containsKey("status")) {
                KeyStatus newStatus = KeyStatus.valueOf(request.get("status").toUpperCase());
                key.setStatus(newStatus);

                String notes = request.getOrDefault("statusChangeNotes",
                        "Status changed to " + newStatus + " by " + username);
                key.setAdminNotes((key.getAdminNotes() != null ? key.getAdminNotes() + "; " : "") + notes);
            }

            DormitoryKey savedKey = keyRepository.save(key);

            log.info("Key {} updated by {}", key.getKeyCode(), username);

            KeyDto keyDto = keyManagementService.getKeyById(savedKey.getId())
                    .orElseThrow(() -> new RuntimeException("Failed to retrieve updated key"));

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Key updated successfully");
            response.put("key", keyDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating key: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to update key", "message", e.getMessage()));
        }
    }

    @GetMapping("/assignments/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getActiveAssignments() {
        try {
            List<KeyAssignmentDto> assignments = keyManagementService.getActiveAssignments();
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("Error retrieving active assignments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve assignments"));
        }
    }

    @GetMapping("/assignments/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAllAssignments() {
        try {
            List<KeyAssignmentDto> assignments = keyManagementService.getAllAssignments();
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("Error retrieving all assignments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve assignments"));
        }
    }

    @GetMapping("/assignments/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getOverdueAssignments() {
        try {
            List<KeyAssignmentDto> assignments = keyManagementService.getOverdueAssignments();
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("Error retrieving overdue assignments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve assignments"));
        }
    }

    @GetMapping("/assignments/my")
    public ResponseEntity<?> getMyAssignments(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<KeyAssignmentDto> assignments = keyManagementService.getUserAssignments(user);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            log.error("Error retrieving user assignments: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve assignments"));
        }
    }

    @PostMapping("/assignments/{assignmentId}/extend")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> extendAssignment(
            @PathVariable Long assignmentId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            LocalDateTime newExpectedReturn = LocalDateTime.parse(request.get("newExpectedReturn"));
            String reason = request.getOrDefault("reason", "Extended by staff");

            KeyAssignmentDto assignment = keyManagementService.extendAssignment(
                    assignmentId, newExpectedReturn, reason, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment extended successfully");
            response.put("assignment", assignment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error extending assignment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to extend assignment", "message", e.getMessage()));
        }
    }

    @GetMapping("/attention")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getKeysNeedingAttention() {
        try {
            List<KeyDto> keys = keyManagementService.getKeysNeedingAttention();
            return ResponseEntity.ok(keys);
        } catch (Exception e) {
            log.error("Error retrieving keys needing attention: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve keys"));
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getKeyStatistics() {
        try {
            KeyManagementService.KeyStatsDto stats = keyManagementService.getKeyStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving key statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve statistics"));
        }
    }
}