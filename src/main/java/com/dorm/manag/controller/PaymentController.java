package com.dorm.manag.controller;

import com.dorm.manag.dto.CreatePaymentRequest;
import com.dorm.manag.dto.PaymentDto;
import com.dorm.manag.entity.PaymentStatus;
import com.dorm.manag.entity.User;
import com.dorm.manag.service.PaymentService;
import com.dorm.manag.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            PaymentDto payment = paymentService.createPayment(request, user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment created successfully");
            response.put("payment", payment);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating payment: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/my-payments")
    public ResponseEntity<?> getMyPayments(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<PaymentDto> payments;
            if (page == 0 && size == 10) {
                payments = paymentService.getUserPaymentHistory(user);
            } else {
                payments = paymentService.getUserPaymentHistory(user, page, size);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("payments", payments);
            response.put("totalPaid", paymentService.getUserTotalPaid(user));
            response.put("totalPending", paymentService.getUserTotalPending(user));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving user payments: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve payments");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAllPayments() {
        try {
            List<PaymentDto> payments = paymentService.getAllPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            log.error("Error retrieving all payments: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve payments");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPendingPayments() {
        try {
            List<PaymentDto> pendingPayments = paymentService.getPendingPayments();
            return ResponseEntity.ok(pendingPayments);
        } catch (Exception e) {
            log.error("Error retrieving pending payments: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve pending payments");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOverduePayments() {
        try {
            List<PaymentDto> overduePayments = paymentService.getOverduePayments();
            return ResponseEntity.ok(overduePayments);
        } catch (Exception e) {
            log.error("Error retrieving overdue payments: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve overdue payments");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentStatistics() {
        try {
            PaymentService.PaymentStatsDto stats = paymentService.getPaymentStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving payment statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve payment statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processPayment(@PathVariable Long id, Authentication authentication) {
        try {
            // Check if user owns the payment or is admin
            Optional<PaymentDto> paymentOpt = paymentService.getPaymentById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PaymentDto existingPayment = paymentOpt.get();
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!existingPayment.getUserId().equals(user.getId()) && !user.getRole().hasAdminPrivileges()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            PaymentDto processedPayment = paymentService.processPayment(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment processed successfully");
            response.put("payment", processedPayment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing payment {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to process payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<PaymentDto> paymentOpt = paymentService.getPaymentById(id);
            if (paymentOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Payment not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            PaymentDto payment = paymentOpt.get();
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!payment.getUserId().equals(user.getId()) && !user.getRole().hasAdminPrivileges()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            log.error("Error retrieving payment {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve payment");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> getPaymentReceipt(@PathVariable Long id, Authentication authentication) {
        try {
            Optional<PaymentDto> paymentOpt = paymentService.getPaymentById(id);
            if (paymentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PaymentDto payment = paymentOpt.get();
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!payment.getUserId().equals(user.getId()) && !user.getRole().hasAdminPrivileges()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            byte[] pdfBytes = paymentService.generatePaymentReceipt(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "payment-receipt-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Error generating receipt for payment {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to generate receipt");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable Long id,
            @RequestParam PaymentStatus status,
            @RequestParam(required = false) String adminNotes) {
        try {
            PaymentDto updatedPayment = paymentService.updatePaymentStatus(id, status, adminNotes);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Payment status updated successfully");
            response.put("payment", updatedPayment);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating payment {} status: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update payment status");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
}