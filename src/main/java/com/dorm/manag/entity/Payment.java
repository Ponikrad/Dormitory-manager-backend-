package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private String description;

    @Column(name = "payment_type")
    private String paymentType; // RENT, UTILITIES, DEPOSIT, FINE, etc.

    @Column(name = "external_payment_id")
    private String externalPaymentId; // ID from payment gateway

    @Column(name = "transaction_id")
    private String transactionId; // Internal transaction ID

    @Column(name = "receipt_url")
    private String receiptUrl; // URL to PDF receipt

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Additional metadata
    @Column(name = "currency", length = 3)
    private String currency = "PLN";

    @Column(name = "room_number")
    private String roomNumber; // For rent payments

    @Column(name = "period_start")
    private LocalDateTime periodStart; // For recurring payments

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    // Constructors
    public Payment(User user, BigDecimal amount, PaymentMethod paymentMethod, String description) {
        this.user = user;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.status = PaymentStatus.PENDING;
        this.currency = "PLN";
        this.roomNumber = user.getRoomNumber();
    }

    public Payment(User user, BigDecimal amount, PaymentMethod paymentMethod, String description, String paymentType) {
        this(user, amount, paymentMethod, description);
        this.paymentType = paymentType;
    }

    // Helper methods
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    public boolean canBeCancelled() {
        return this.status.canBeCancelled();
    }

    public boolean canBeRefunded() {
        return this.status.canBeRefunded();
    }

    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }

    public boolean isOverdue() {
        return this.dueDate != null && LocalDateTime.now().isAfter(this.dueDate) && !isCompleted();
    }

    public long getDaysUntilDue() {
        if (dueDate == null || isCompleted())
            return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dueDate);
    }

    public String getDisplayAmount() {
        return String.format("%.2f %s", amount, currency);
    }

    // Override toString to avoid circular reference
    @Override
    public String toString() {
        return "Payment{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", createdAt=" + createdAt +
                ", completedAt=" + completedAt +
                '}';
    }
}