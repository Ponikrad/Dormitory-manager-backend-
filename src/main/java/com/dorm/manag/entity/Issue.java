package com.dorm.manag.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // User who reported the issue

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.REPORTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssuePriority priority = IssuePriority.MEDIUM;

    @Column(name = "room_number")
    private String roomNumber;

    @Column(name = "location_details")
    private String locationDetails; // e.g., "bathroom", "kitchen", "common area"

    @CreationTimestamp
    @Column(name = "reported_at", nullable = false, updatable = false)
    private LocalDateTime reportedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo; // Staff member assigned to resolve the issue

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Additional tracking fields
    @Column(name = "estimated_resolution")
    private LocalDateTime estimatedResolution;

    @Column(name = "actual_resolution_time_hours")
    private Integer actualResolutionTimeHours;

    @Column(name = "contractor_required")
    private Boolean contractorRequired = false;

    @Column(name = "cost_estimate")
    private String costEstimate;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "user_satisfaction_rating")
    private Integer userSatisfactionRating; // 1-5 stars

    @Column(name = "reopened_count")
    private Integer reopenedCount = 0;

    // Constructors
    public Issue(User user, String title, String description, IssueCategory category) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.category = category;
        this.status = IssueStatus.REPORTED;
        this.priority = IssuePriority.determinePriority(category, description);
        this.roomNumber = user.getRoomNumber();
        this.reopenedCount = 0;
        this.contractorRequired = category.requiresExternalContractor();
    }

    public Issue(User user, String title, String description, IssueCategory category, IssuePriority priority) {
        this(user, title, description, category);
        this.priority = priority;
    }

    // Helper methods
    public boolean isOpen() {
        return !status.isClosed();
    }

    public boolean isClosed() {
        return status.isClosed();
    }

    public boolean isResolved() {
        return status == IssueStatus.RESOLVED;
    }

    public boolean isOverdue() {
        if (isClosed() || estimatedResolution == null)
            return false;
        return LocalDateTime.now().isAfter(estimatedResolution);
    }

    public void acknowledge() {
        this.status = IssueStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        calculateEstimatedResolution();
    }

    public void startProgress() {
        this.status = IssueStatus.IN_PROGRESS;
    }

    public void resolve(String resolutionNotes) {
        this.status = IssueStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = resolutionNotes;
        calculateActualResolutionTime();
    }

    public void cancel() {
        this.status = IssueStatus.CANCELLED;
    }

    public void escalate() {
        this.status = IssueStatus.ESCALATED;
        this.priority = IssuePriority.URGENT;
    }

    public void reopen() {
        if (canBeReopened()) {
            this.status = IssueStatus.REPORTED;
            this.reopenedCount++;
            this.resolvedAt = null;
            this.resolutionNotes = null;
            calculateEstimatedResolution();
        }
    }

    public boolean canBeReopened() {
        return status.canBeReopened();
    }

    public void assignTo(User staff) {
        this.assignedTo = staff;
        if (this.status == IssueStatus.REPORTED) {
            acknowledge();
        }
    }

    public long getHoursSinceReported() {
        return java.time.temporal.ChronoUnit.HOURS.between(reportedAt, LocalDateTime.now());
    }

    public long getHoursUntilDue() {
        if (estimatedResolution == null || isClosed())
            return 0;
        return java.time.temporal.ChronoUnit.HOURS.between(LocalDateTime.now(), estimatedResolution);
    }

    public boolean requiresUrgentAttention() {
        return priority.requiresImmediateAttention() || isOverdue();
    }

    private void calculateEstimatedResolution() {
        if (acknowledgedAt != null) {
            this.estimatedResolution = acknowledgedAt.plusHours(priority.getRecommendedResolutionTimeHours());
        }
    }

    private void calculateActualResolutionTime() {
        if (reportedAt != null && resolvedAt != null) {
            this.actualResolutionTimeHours = (int) java.time.temporal.ChronoUnit.HOURS.between(reportedAt, resolvedAt);
        }
    }

    public String getStatusDisplay() {
        return status.getDisplayName();
    }

    public String getPriorityDisplay() {
        return priority.getDisplayName();
    }

    public String getCategoryDisplay() {
        return category.getDisplayName();
    }

    // Override toString to avoid circular reference
    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", status=" + status +
                ", priority=" + priority +
                ", roomNumber='" + roomNumber + '\'' +
                ", reportedAt=" + reportedAt +
                ", assignedTo=" + (assignedTo != null ? assignedTo.getId() : null) +
                '}';
    }
}