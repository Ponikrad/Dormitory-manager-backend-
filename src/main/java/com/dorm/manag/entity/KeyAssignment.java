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
@Table(name = "key_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "key_id", nullable = false)
    private DormitoryKey key;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id", nullable = false)
    private User issuedBy; // Receptionist who issued the key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_to_id")
    private User returnedTo; // Receptionist who received the key back

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type")
    private AssignmentType assignmentType;

    // Timestamps
    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expected_return")
    private LocalDateTime expectedReturn;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Status tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status = AssignmentStatus.ACTIVE;

    @Column(name = "is_overdue", nullable = false)
    private Boolean isOverdue = false;

    // Financial
    @Column(name = "deposit_amount", precision = 8, scale = 2)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "deposit_paid", nullable = false)
    private Boolean depositPaid = false;

    @Column(name = "deposit_refunded", nullable = false)
    private Boolean depositRefunded = false;

    @Column(name = "fine_amount", precision = 8, scale = 2)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @Column(name = "replacement_cost", precision = 8, scale = 2)
    private BigDecimal replacementCost = BigDecimal.ZERO;

    // Associated reservation (for temporary assignments)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    // Notes and conditions
    @Column(name = "issue_notes")
    private String issueNotes;

    @Column(name = "return_notes")
    private String returnNotes;

    @Column(name = "condition_on_issue")
    private String conditionOnIssue = "Good";

    @Column(name = "condition_on_return")
    private String conditionOnReturn;

    @Column(name = "special_conditions", columnDefinition = "TEXT")
    private String specialConditions;

    // Tracking
    @Column(name = "reminder_sent_count")
    private Integer reminderSentCount = 0;

    @Column(name = "last_reminder_sent")
    private LocalDateTime lastReminderSent;

    @Column(name = "extension_count")
    private Integer extensionCount = 0;

    // Constructors
    public KeyAssignment(DormitoryKey key, User user, User issuedBy, AssignmentType type) {
        this.key = key;
        this.user = user;
        this.issuedBy = issuedBy;
        this.assignmentType = type;
        this.status = AssignmentStatus.ACTIVE;

        if (key.getRequiresDeposit()) {
            this.depositAmount = key.getDepositAmount();
        }

        // Set expected return based on assignment type
        if (type == AssignmentType.TEMPORARY) {
            this.expectedReturn = LocalDateTime.now().plusHours(key.getKeyType().getMaxIssueDurationHours());
        }
    }

    // Helper methods
    public boolean isActive() {
        return status == AssignmentStatus.ACTIVE;
    }

    public boolean isReturned() {
        return status == AssignmentStatus.RETURNED;
    }

    public boolean isLost() {
        return status == AssignmentStatus.LOST;
    }

    public boolean isOverdueNow() {
        return expectedReturn != null &&
                LocalDateTime.now().isAfter(expectedReturn) &&
                isActive();
    }

    public void returnKey(User returnedTo, String condition, String notes) {
        this.returnedAt = LocalDateTime.now();
        this.returnedTo = returnedTo;
        this.conditionOnReturn = condition;
        this.returnNotes = notes;
        this.status = AssignmentStatus.RETURNED;

        // Calculate fines if overdue
        if (isOverdueNow()) {
            calculateOverdueFine();
        }

        // Handle deposit refund
        if (depositPaid && !depositRefunded) {
            if ("Good".equals(condition) && fineAmount.compareTo(BigDecimal.ZERO) == 0) {
                this.depositRefunded = true;
            }
        }
    }

    public void reportLost() {
        this.status = AssignmentStatus.LOST;
        this.replacementCost = key.getReplacementCost() != null ? key.getReplacementCost() : BigDecimal.valueOf(100); // Default
                                                                                                                      // replacement
                                                                                                                      // cost

        // Forfeit deposit
        if (depositPaid) {
            this.depositRefunded = false; // Deposit is forfeited
        }
    }

    public void extend(LocalDateTime newExpectedReturn, String reason) {
        if (!isActive()) {
            throw new IllegalStateException("Can only extend active assignments");
        }

        this.expectedReturn = newExpectedReturn;
        this.extensionCount++;
        this.specialConditions = (this.specialConditions != null ? this.specialConditions + "; " : "") +
                "Extended until " + newExpectedReturn + " - Reason: " + reason;
    }

    public void sendReminder() {
        this.reminderSentCount++;
        this.lastReminderSent = LocalDateTime.now();
    }

    public long getHoursOverdue() {
        if (!isOverdueNow())
            return 0;
        return java.time.temporal.ChronoUnit.HOURS.between(expectedReturn, LocalDateTime.now());
    }

    public long getDaysOverdue() {
        return getHoursOverdue() / 24;
    }

    public long getTotalDaysAssigned() {
        LocalDateTime endTime = returnedAt != null ? returnedAt : LocalDateTime.now();
        return java.time.temporal.ChronoUnit.DAYS.between(issuedAt, endTime);
    }

    private void calculateOverdueFine() {
        long daysOverdue = getDaysOverdue();
        if (daysOverdue > 0) {
            // 10 PLN per day overdue
            this.fineAmount = BigDecimal.valueOf(daysOverdue * 10);
        }
    }

    public BigDecimal getTotalAmountOwed() {
        BigDecimal total = BigDecimal.ZERO;

        // Add fine amount
        if (fineAmount != null && fineAmount.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(fineAmount);
        }

        // Add replacement cost if lost
        if (isLost() && replacementCost != null && replacementCost.compareTo(BigDecimal.ZERO) > 0) {
            total = total.add(replacementCost);
        }

        // Subtract refundable deposit if applicable
        if (!depositRefunded && depositPaid && !isLost() &&
                (fineAmount == null || fineAmount.compareTo(BigDecimal.ZERO) == 0)) {
            total = total.subtract(depositAmount);
        }

        return total.max(BigDecimal.ZERO);
    }

    public boolean needsReminder() {
        if (!isActive() || expectedReturn == null)
            return false;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTime = expectedReturn.minusHours(2); // Remind 2 hours before

        return now.isAfter(reminderTime) &&
                (lastReminderSent == null || lastReminderSent.isBefore(now.minusHours(1)));
    }

    public String getAssignmentSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(key.getDisplayName())
                .append(" assigned to ")
                .append(user.getFirstName()).append(" ").append(user.getLastName());

        if (assignmentType == AssignmentType.TEMPORARY && expectedReturn != null) {
            summary.append(" until ").append(expectedReturn.toLocalDate());
        }

        if (isOverdueNow()) {
            summary.append(" (OVERDUE by ").append(getDaysOverdue()).append(" days)");
        }

        return summary.toString();
    }

    // Enums for assignment
    public enum AssignmentType {
        PERMANENT("Permanent Assignment"),
        TEMPORARY("Temporary Assignment"),
        EMERGENCY("Emergency Assignment");

        private final String displayName;

        AssignmentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum AssignmentStatus {
        ACTIVE("Active"),
        RETURNED("Returned"),
        LOST("Lost"),
        CANCELLED("Cancelled");

        private final String displayName;

        AssignmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "KeyAssignment{" +
                "id=" + id +
                ", keyId=" + (key != null ? key.getId() : null) +
                ", userId=" + (user != null ? user.getId() : null) +
                ", assignmentType=" + assignmentType +
                ", status=" + status +
                ", issuedAt=" + issuedAt +
                ", expectedReturn=" + expectedReturn +
                ", returnedAt=" + returnedAt +
                ", isOverdue=" + isOverdueNow() +
                '}';
    }
}