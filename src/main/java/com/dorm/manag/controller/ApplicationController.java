package com.dorm.manag.controller;

import com.dorm.manag.dto.ApplicationDto;
import com.dorm.manag.dto.CreateApplicationRequest;
import com.dorm.manag.entity.ApplicationStatus;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.ApplicationService;
import com.dorm.manag.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    // PUBLIC/STUDENTS

    @PostMapping("/submit")
    public ResponseEntity<?> submitApplication(@Valid @RequestBody CreateApplicationRequest request) {
        try {
            ApplicationDto application = applicationService.submitApplication(request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application submitted successfully");
            response.put("application", application);
            response.put("applicationNumber", application.getApplicationNumber());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error submitting application: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to submit application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkApplicationByEmail(@RequestParam String email) {
        try {
            Optional<ApplicationDto> application = applicationService.getApplicationByEmail(email);

            if (application.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No application found for this email");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(application.get());
        } catch (Exception e) {
            log.error("Error checking application: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to check application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ADMIN

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllApplications() {
        try {
            List<ApplicationDto> applications = applicationService.getAllApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("Error retrieving applications: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve applications");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingApplications() {
        try {
            List<ApplicationDto> applications = applicationService.getPendingApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("Error retrieving pending applications: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve pending applications");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/by-status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationsByStatus(@PathVariable ApplicationStatus status) {
        try {
            List<ApplicationDto> applications = applicationService.getApplicationsByStatus(status);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("Error retrieving applications by status: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve applications");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOverdueApplications() {
        try {
            List<ApplicationDto> applications = applicationService.getOverdueApplications();
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("Error retrieving overdue applications: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve overdue applications");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationStatistics() {
        try {
            ApplicationService.ApplicationStatsDto stats = applicationService.getApplicationStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving application statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getApplicationById(@PathVariable Long id) {
        try {
            Optional<ApplicationDto> application = applicationService.getApplicationById(id);

            if (application.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Application not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            return ResponseEntity.ok(application.get());
        } catch (Exception e) {
            log.error("Error retrieving application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateApplication(@PathVariable Long id,
            @Valid @RequestBody CreateApplicationRequest request) {
        try {
            ApplicationDto application = applicationService.updateApplication(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application updated successfully");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveApplication(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            User approver = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ApplicationDto application = applicationService.approveApplication(id, approver);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application approved successfully");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error approving application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to approve application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectApplication(@PathVariable Long id,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User reviewer = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ApplicationDto application = applicationService.rejectApplication(id, reason, reviewer);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application rejected");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error rejecting application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to reject application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmApplication(@PathVariable Long id) {
        try {
            ApplicationDto application = applicationService.confirmApplication(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application confirmed");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error confirming application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to confirm application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/assign-room")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignRoom(@PathVariable Long id,
            @RequestParam String roomNumber,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User assignedBy = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ApplicationDto application = applicationService.assignRoom(id, roomNumber, assignedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Room assigned successfully. User account created.");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error assigning room to application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to assign room");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/request-documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> requestDocuments(@PathVariable Long id,
            @RequestParam String missingDocuments,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User requestedBy = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ApplicationDto application = applicationService.requestDocuments(id, missingDocuments, requestedBy);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Documents requested");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error requesting documents for application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to request documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> completeApplication(@PathVariable Long id) {
        try {
            ApplicationDto application = applicationService.completeApplication(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application completed");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error completing application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to complete application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdrawApplication(@PathVariable Long id) {
        try {
            ApplicationDto application = applicationService.withdrawApplication(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Application withdrawn");
            response.put("application", application);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error withdrawing application {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to withdraw application");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}