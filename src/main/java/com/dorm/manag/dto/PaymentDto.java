package com.dorm.manag.dto;

import com.dorm.manag.entity.PaymentMethod;
import com.dorm.manag.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;

    private Long userId;

    private String userFullName;

    private String userEmail;

    private BigDecimal amount;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private String description;

    private String paymentType;

    private String externalPaymentId;

    private String transactionId;

    private String receiptUrl;

    private String currency;

    private String roomNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime periodEnd;

    private String failureReason;

    private boolean overdue;

    private long daysUntilDue;

    private String displayAmount;

    private String statusDisplay;

    public void calculateFields() {
        this.overdue = dueDate != null && LocalDateTime.now().isAfter(dueDate) &&
                status != PaymentStatus.COMPLETED;

        if (dueDate != null && status != PaymentStatus.COMPLETED) {
            this.daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
        } else {
            this.daysUntilDue = 0;
        }

        this.displayAmount = String.format("%.2f %s", amount, currency != null ? currency : "PLN");
        this.statusDisplay = status != null ? status.getDisplayName() : "Unknown";
    }

    public static PaymentDto fromBasicData(Long userId, BigDecimal amount, PaymentMethod method, String description) {
        PaymentDto dto = new PaymentDto();
        dto.setUserId(userId);
        dto.setAmount(amount);
        dto.setPaymentMethod(method);
        dto.setDescription(description);
        dto.setStatus(PaymentStatus.PENDING);
        dto.setCurrency("PLN");
        dto.setCreatedAt(LocalDateTime.now());
        dto.calculateFields();
        return dto;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private PaymentDto dto = new PaymentDto();

        public Builder id(Long id) {
            dto.setId(id);
            return this;
        }

        public Builder userId(Long userId) {
            dto.setUserId(userId);
            return this;
        }

        public Builder userFullName(String userFullName) {
            dto.setUserFullName(userFullName);
            return this;
        }

        public Builder amount(BigDecimal amount) {
            dto.setAmount(amount);
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            dto.setPaymentMethod(paymentMethod);
            return this;
        }

        public Builder status(PaymentStatus status) {
            dto.setStatus(status);
            return this;
        }

        public Builder description(String description) {
            dto.setDescription(description);
            return this;
        }

        public Builder paymentType(String paymentType) {
            dto.setPaymentType(paymentType);
            return this;
        }

        public Builder dueDate(LocalDateTime dueDate) {
            dto.setDueDate(dueDate);
            return this;
        }

        public Builder roomNumber(String roomNumber) {
            dto.setRoomNumber(roomNumber);
            return this;
        }

        public PaymentDto build() {
            dto.calculateFields();
            return dto;
        }
    }
}