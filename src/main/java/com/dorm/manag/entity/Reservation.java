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
@Table(name = "reservations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ReservableResource resource;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "number_of_people")
    private Integer numberOfPeople = 1;

    @Column(columnDefinition = "TEXT")
    private String notes; // User notes/special requirements

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes; // Admin notes (approval/rejection reasons)

    // Timestamps
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // Payment information
    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "payment_status")
    private String paymentStatus; // UNPAID, PAID, REFUNDED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment; // Link to payment if cost > 0

    // Additional tracking
    @Column(name = "check_in_code")
    private String checkInCode; // QR code or PIN for self check-in

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy; // Admin who approved

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "late_fee", precision = 8, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime; // When user actually finished

    // Key pickup/return tracking
    @Column(name = "key_picked_up")
    private Boolean keyPickedUp = false;

    @Column(name = "key_picked_up_at")
    private LocalDateTime keyPickedUpAt;

    @Column(name = "key_picked_up_by")
    private String keyPickedUpBy;

    @Column(name = "key_returned")
    private Boolean keyReturned = false;

    @Column(name = "key_returned_at")
    private LocalDateTime keyReturnedAt;

    @Column(name = "key_returned_to")
    private String keyReturnedTo;

    // Constructors
    public Reservation(User user, ReservableResource resource, LocalDateTime startTime, LocalDateTime endTime) {
        this.user = user;
        this.resource = resource;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = resource.getRequiresApproval() ? ReservationStatus.PENDING : ReservationStatus.CONFIRMED;
        this.numberOfPeople = 1;

        // Calculate cost
        calculateTotalCost();

        // Set payment status
        this.paymentStatus = totalCost.compareTo(BigDecimal.ZERO) > 0 ? "UNPAID" : "PAID";
    }

    // Helper methods
    public int getDurationMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    public boolean isActive() {
        return status.isActive();
    }

    public boolean isFinal() {
        return status.isFinal();
    }

    public boolean canBeCancelled() {
        if (!status.canBeCancelled())
            return false;

        // Allow cancellation as long as reservation hasn't started yet
        return LocalDateTime.now().isBefore(startTime);
    }

    public boolean canCheckIn() {
        if (!status.canCheckIn())
            return false;

        // Check if current time is within check-in window (15 minutes before to 30
        // minutes after start)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkInStart = startTime.minusMinutes(15);
        LocalDateTime checkInEnd = startTime.plusMinutes(30);

        return now.isAfter(checkInStart) && now.isBefore(checkInEnd);
    }

    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(endTime) &&
                (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CONFIRMED);
    }

    public boolean isUpcoming() {
        return status == ReservationStatus.CONFIRMED &&
                LocalDateTime.now().isBefore(startTime);
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void checkIn() {
        this.status = ReservationStatus.CHECKED_IN;
        this.checkedInAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.actualEndTime = LocalDateTime.now();

        // Calculate late fee if applicable
        if (actualEndTime.isAfter(endTime)) {
            calculateLateFee();
        }
    }

    public void cancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    public void reject(String reason) {
        this.status = ReservationStatus.REJECTED;
        this.adminNotes = reason;
    }

    public void markAsNoShow() {
        this.status = ReservationStatus.NO_SHOW;

        // Apply no-show penalty if configured
        if (depositAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = "FORFEITED";
        }
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
    }

    public void approve(User approver) {
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
        this.approvedBy = approver;
    }

    public void pickUpKey(String receptionistName) {
        this.keyPickedUp = true;
        this.keyPickedUpAt = LocalDateTime.now();
        this.keyPickedUpBy = receptionistName;

        // Auto check-in when key is picked up
        if (status == ReservationStatus.CONFIRMED) {
            checkIn();
        }
    }

    public void returnKey(String receptionistName) {
        this.keyReturned = true;
        this.keyReturnedAt = LocalDateTime.now();
        this.keyReturnedTo = receptionistName;

        // Auto complete when key is returned
        if (status == ReservationStatus.CHECKED_IN) {
            complete();
        }
    }

    public boolean canPickUpKey() {
        return status == ReservationStatus.CONFIRMED &&
                !keyPickedUp &&
                resource.getRequiresKey() &&
                canCheckIn(); // Same time window as check-in
    }

    public boolean needsKeyReturn() {
        return keyPickedUp && !keyReturned && resource.getRequiresKey();
    }

    public long getMinutesUntilStart() {
        return java.time.Duration.between(LocalDateTime.now(), startTime).toMinutes();
    }

    public long getMinutesSinceStart() {
        return java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes();
    }

    public boolean overlapsWidth(LocalDateTime otherStart, LocalDateTime otherEnd) {
        return startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }

    private void calculateTotalCost() {
        if (resource.getCostPerHour().compareTo(BigDecimal.ZERO) == 0) {
            this.totalCost = BigDecimal.ZERO;
            return;
        }

        double hours = getDurationMinutes() / 60.0;
        this.totalCost = resource.getCostPerHour().multiply(BigDecimal.valueOf(hours));

        // Add deposit if required
        if (resource.getDepositRequired().compareTo(BigDecimal.ZERO) > 0) {
            this.depositAmount = resource.getDepositRequired();
            this.totalCost = this.totalCost.add(depositAmount);
        }
    }

    private void calculateLateFee() {
        if (actualEndTime == null || !actualEndTime.isAfter(endTime)) {
            return;
        }

        long minutesLate = java.time.Duration.between(endTime, actualEndTime).toMinutes();

        // 10 PLN per 30 minutes late (or part thereof)
        long penaltyPeriods = (minutesLate + 29) / 30; // Round up
        this.lateFee = BigDecimal.valueOf(penaltyPeriods * 10);
    }

    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    public String getFormattedDuration() {
        int minutes = getDurationMinutes();
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, remainingMinutes);
        } else {
            return String.format("%dm", remainingMinutes);
        }
    }

    // Override toString to avoid circular references
    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", resourceId=" + (resource != null ? resource.getId() : null) +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", totalCost=" + totalCost +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}