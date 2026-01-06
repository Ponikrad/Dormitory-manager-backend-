package com.dorm.manag.service;

import com.dorm.manag.dto.CreatePaymentRequest;
import com.dorm.manag.dto.PaymentDto;
import com.dorm.manag.entity.Payment;
import com.dorm.manag.entity.PaymentStatus;
import com.dorm.manag.entity.User;
import com.dorm.manag.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PdfService pdfService;

    @Transactional
    public PaymentDto createPayment(CreatePaymentRequest request, User user) {
        log.info("Creating payment for user: {} amount: {}", user.getUsername(), request.getAmount());

        validatePaymentRequest(request);

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setDescription(request.getDescription());
        payment.setPaymentType(request.getPaymentType());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCurrency(request.getCurrency());
        payment.setRoomNumber(user.getRoomNumber());
        payment.setDueDate(request.getDueDate());
        payment.setPeriodStart(request.getPeriodStart());
        payment.setPeriodEnd(request.getPeriodEnd());

        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", savedPayment.getId());

        return convertToDto(savedPayment);
    }

    @Transactional
    public PaymentDto processPayment(Long paymentId) {
        log.info("Processing payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        if (!payment.isPending()) {
            throw new RuntimeException("Payment is not in pending status");
        }

        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        try {
            Thread.sleep(1000);

            boolean success = Math.random() > 0.1;

            if (success) {
                return completePayment(payment);
            } else {
                return failPayment(payment, "Payment declined by bank");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failPayment(payment, "Processing interrupted");
        }
    }

    @Transactional
    public PaymentDto completePayment(Payment payment) {
        payment.markAsCompleted();
        payment.setExternalPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());

        Payment completedPayment = paymentRepository.save(payment);

        try {
            // Generate receipt using ID to ensure User is loaded
            generatePaymentReceipt(completedPayment.getId());
        } catch (Exception e) {
            log.error("Failed to generate receipt for payment {}: {}", payment.getId(), e.getMessage());
        }

        log.info("Payment completed: {}", completedPayment.getId());
        return convertToDto(completedPayment);
    }

    @Transactional
    public PaymentDto failPayment(Payment payment, String reason) {
        payment.markAsFailed(reason);
        Payment failedPayment = paymentRepository.save(payment);

        log.warn("Payment failed: {} - {}", failedPayment.getId(), reason);
        return convertToDto(failedPayment);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getUserPaymentHistory(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Payment> payments = paymentRepository.findByUserOrderByCreatedAtDesc(user, pageable);

        return payments.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getUserPaymentHistory(User user) {
        List<Payment> payments = paymentRepository.findByUserOrderByCreatedAtDesc(user);

        return payments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDto> getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getPendingPayments() {
        List<Payment> pendingPayments = paymentRepository.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING);

        return pendingPayments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getOverduePayments() {
        List<Payment> overduePayments = paymentRepository.findOverduePayments(LocalDateTime.now());

        return overduePayments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> getAllPayments() {
        List<Payment> allPayments = paymentRepository.findAll();

        // Filter out payments for admin and receptionist users
        return allPayments.stream()
                .filter(p -> {
                    User user = p.getUser();
                    return user != null &&
                            user.getRole() != com.dorm.manag.entity.Role.ADMIN &&
                            user.getRole() != com.dorm.manag.entity.Role.RECEPTIONIST;
                })
                .map(this::convertToDto)
                .sorted((p1, p2) -> {
                    if (p1.getCreatedAt() == null || p2.getCreatedAt() == null)
                        return 0;
                    return p2.getCreatedAt().compareTo(p1.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentDto updatePaymentStatus(Long paymentId, PaymentStatus newStatus, String adminNotes) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.COMPLETED && oldStatus != PaymentStatus.COMPLETED) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        Payment updatedPayment = paymentRepository.save(payment);

        if (newStatus == PaymentStatus.COMPLETED && oldStatus != PaymentStatus.COMPLETED) {
            // Generate receipt using ID to ensure User is loaded
            try {
                generatePaymentReceipt(updatedPayment.getId());
            } catch (Exception e) {
                log.error("Failed to generate receipt for payment {}: {}", updatedPayment.getId(), e.getMessage());
            }
        }

        log.info("Payment {} status updated from {} to {}", paymentId, oldStatus, newStatus);
        return convertToDto(updatedPayment);
    }

    @Transactional
    public byte[] generatePaymentReceipt(Long paymentId) {
        // Use JOIN FETCH to eagerly load User to avoid LazyInitializationException
        Payment payment = paymentRepository.findByIdWithUser(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Generate PDF while transaction is still active (User is loaded)
        byte[] pdfBytes = pdfService.generatePaymentReceipt(payment);

        // Update receipt URL
        String receiptUrl = "/api/payments/" + payment.getId() + "/receipt";
        payment.setReceiptUrl(receiptUrl);
        paymentRepository.save(payment);

        return pdfBytes;
    }

    @Transactional(readOnly = true)
    public PaymentStatsDto getPaymentStatistics() {
        long totalCompleted = paymentRepository.countCompletedPayments();
        long totalPending = paymentRepository.countPendingPayments();
        long totalFailed = paymentRepository.countFailedPayments();

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59).withSecond(59);

        Optional<BigDecimal> monthlyTotal = paymentRepository.getTotalCompletedAmountBetween(startOfMonth, endOfMonth);

        PaymentStatsDto stats = new PaymentStatsDto();
        stats.setTotalCompleted(totalCompleted);
        stats.setTotalPending(totalPending);
        stats.setTotalFailed(totalFailed);
        stats.setMonthlyTotal(monthlyTotal.orElse(BigDecimal.ZERO));

        return stats;
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalPaid(User user) {
        return paymentRepository.getTotalPaidByUser(user.getId()).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public BigDecimal getUserTotalPending(User user) {
        return paymentRepository.getTotalPendingByUser(user.getId()).orElse(BigDecimal.ZERO);
    }

    private void validatePaymentRequest(CreatePaymentRequest request) {
        if (!request.isValid()) {
            throw new IllegalArgumentException("Invalid payment request");
        }

        if (!request.isValidAmount()) {
            throw new IllegalArgumentException("Invalid payment amount");
        }

        if (!request.isValidDueDate()) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }

        if (!request.isValidPeriod()) {
            throw new IllegalArgumentException("Invalid payment period");
        }
    }

    private PaymentDto convertToDto(Payment payment) {
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setUserId(payment.getUser().getId());
        dto.setUserFullName(payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
        dto.setUserEmail(payment.getUser().getEmail());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setDescription(payment.getDescription());
        dto.setPaymentType(payment.getPaymentType());
        dto.setExternalPaymentId(payment.getExternalPaymentId());
        dto.setTransactionId(payment.getTransactionId());
        dto.setReceiptUrl(payment.getReceiptUrl());
        dto.setCurrency(payment.getCurrency());
        dto.setRoomNumber(payment.getRoomNumber());
        dto.setDueDate(payment.getDueDate());
        dto.setCompletedAt(payment.getCompletedAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setPeriodStart(payment.getPeriodStart());
        dto.setPeriodEnd(payment.getPeriodEnd());
        dto.setFailureReason(payment.getFailureReason());

        dto.calculateFields();
        return dto;
    }

    @lombok.Data
    public static class PaymentStatsDto {
        private long totalCompleted;
        private long totalPending;
        private long totalFailed;
        private BigDecimal monthlyTotal;
        private BigDecimal yearlyTotal;
    }
}