package com.dorm.manag.dto;

import com.dorm.manag.entity.IssueCategory;
import com.dorm.manag.entity.IssuePriority;
import com.dorm.manag.entity.IssueStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueDto {

    private Long id;

    private Long userId;

    private String userFullName;

    private String userEmail;

    private String title;

    private String description;

    private IssueCategory category;

    private IssueStatus status;

    private IssuePriority priority;

    private String roomNumber;

    private String locationDetails;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reportedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acknowledgedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime estimatedResolution;

    private String adminNotes;

    private Long assignedToUserId;

    private String assignedToUserName;

    private Boolean contractorRequired;

    private String costEstimate;

    private String resolutionNotes;

    private Integer userSatisfactionRating;

    private Integer reopenedCount;

    private Integer actualResolutionTimeHours;

    private boolean overdue;

    private long hoursSinceReported;

    private long hoursUntilDue;

    private boolean requiresUrgentAttention;

    private String statusDisplay;

    private String priorityDisplay;

    private String categoryDisplay;

    public void calculateFields() {
        LocalDateTime now = LocalDateTime.now();

        if (reportedAt != null) {
            this.hoursSinceReported = java.time.temporal.ChronoUnit.HOURS.between(reportedAt, now);
        }

        if (estimatedResolution != null && !status.isClosed()) {
            this.overdue = now.isAfter(estimatedResolution);
            this.hoursUntilDue = java.time.temporal.ChronoUnit.HOURS.between(now, estimatedResolution);
            if (hoursUntilDue < 0)
                hoursUntilDue = 0;
        } else {
            this.overdue = false;
            this.hoursUntilDue = 0;
        }

        this.requiresUrgentAttention = (priority != null && priority.requiresImmediateAttention()) || overdue;

        this.statusDisplay = status != null ? status.getDisplayName() : "Unknown";
        this.priorityDisplay = priority != null ? priority.getDisplayName() : "Unknown";
        this.categoryDisplay = category != null ? category.getDisplayName() : "Unknown";
    }

    public boolean isOpen() {
        return status != null && !status.isClosed();
    }

    public boolean isClosed() {
        return status != null && status.isClosed();
    }

    public boolean isResolved() {
        return status == IssueStatus.RESOLVED;
    }

    public boolean canBeReopened() {
        return status != null && status.canBeReopened();
    }

    public String getFormattedReportedTime() {
        if (reportedAt == null)
            return "Unknown";

        long hours = hoursSinceReported;
        if (hours < 24) {
            return hours + " hours ago";
        } else {
            long days = hours / 24;
            return days + " days ago";
        }
    }

    public String getFormattedTimeUntilDue() {
        if (hoursUntilDue <= 0)
            return "Overdue";

        if (hoursUntilDue < 24) {
            return hoursUntilDue + " hours left";
        } else {
            long days = hoursUntilDue / 24;
            return days + " days left";
        }
    }

    public String getUrgencyIndicator() {
        if (requiresUrgentAttention) {
            return overdue ? "OVERDUE" : "URGENT";
        }
        return priority != null ? priority.getDisplayName().toUpperCase() : "NORMAL";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IssueDto dto = new IssueDto();

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

        public Builder title(String title) {
            dto.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            dto.setDescription(description);
            return this;
        }

        public Builder category(IssueCategory category) {
            dto.setCategory(category);
            return this;
        }

        public Builder status(IssueStatus status) {
            dto.setStatus(status);
            return this;
        }

        public Builder priority(IssuePriority priority) {
            dto.setPriority(priority);
            return this;
        }

        public Builder roomNumber(String roomNumber) {
            dto.setRoomNumber(roomNumber);
            return this;
        }

        public Builder reportedAt(LocalDateTime reportedAt) {
            dto.setReportedAt(reportedAt);
            return this;
        }

        public Builder assignedToUserId(Long assignedToUserId) {
            dto.setAssignedToUserId(assignedToUserId);
            return this;
        }

        public Builder assignedToUserName(String assignedToUserName) {
            dto.setAssignedToUserName(assignedToUserName);
            return this;
        }

        public IssueDto build() {
            dto.calculateFields();
            return dto;
        }
    }
}