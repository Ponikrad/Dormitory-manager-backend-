package com.dorm.manag.entity;

/**
 * Enum representing the status of dormitory applications
 */
public enum ApplicationStatus {
    /**
     * Application has been submitted but not yet reviewed
     */
    SUBMITTED("Submitted"),

    /**
     * Application is under review by admissions committee
     */
    UNDER_REVIEW("Under Review"),

    /**
     * Application requires additional documents or information
     */
    PENDING_DOCUMENTS("Pending Documents"),

    /**
     * Application has been approved, waiting for confirmation
     */
    APPROVED("Approved"),

    /**
     * Application has been rejected
     */
    REJECTED("Rejected"),

    /**
     * Student has confirmed acceptance and paid fees
     */
    CONFIRMED("Confirmed"),

    /**
     * Student has been assigned a room
     */
    ROOM_ASSIGNED("Room Assigned"),

    /**
     * Student has completed move-in process
     */
    COMPLETED("Completed"),

    /**
     * Application was withdrawn by student
     */
    WITHDRAWN("Withdrawn"),

    /**
     * Application was cancelled by administration
     */
    CANCELLED("Cancelled");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if application is in pending state (awaiting action)
     */
    public boolean isPending() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == PENDING_DOCUMENTS;
    }

    /**
     * Check if application is approved
     */
    public boolean isApproved() {
        return this == APPROVED || this == CONFIRMED || this == ROOM_ASSIGNED || this == COMPLETED;
    }

    /**
     * Check if application is in final state
     */
    public boolean isFinal() {
        return this == REJECTED || this == COMPLETED || this == WITHDRAWN || this == CANCELLED;
    }

    /**
     * Check if student can still modify application
     */
    public boolean canBeModified() {
        return this == SUBMITTED || this == PENDING_DOCUMENTS;
    }

    /**
     * Check if application requires student action
     */
    public boolean requiresStudentAction() {
        return this == PENDING_DOCUMENTS || this == APPROVED;
    }

    /**
     * Check if application requires admin action
     */
    public boolean requiresAdminAction() {
        return this == SUBMITTED || this == UNDER_REVIEW;
    }

    /**
     * Get next possible statuses from current status
     */
    public ApplicationStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case SUBMITTED -> new ApplicationStatus[] { UNDER_REVIEW, PENDING_DOCUMENTS, REJECTED, WITHDRAWN };
            case UNDER_REVIEW -> new ApplicationStatus[] { APPROVED, REJECTED, PENDING_DOCUMENTS };
            case PENDING_DOCUMENTS -> new ApplicationStatus[] { UNDER_REVIEW, WITHDRAWN };
            case APPROVED -> new ApplicationStatus[] { CONFIRMED, WITHDRAWN, CANCELLED };
            case CONFIRMED -> new ApplicationStatus[] { ROOM_ASSIGNED, CANCELLED };
            case ROOM_ASSIGNED -> new ApplicationStatus[] { COMPLETED, CANCELLED };
            case REJECTED, COMPLETED, WITHDRAWN, CANCELLED -> new ApplicationStatus[] {}; // Final states
        };
    }

    /**
     * Get expected processing time in days
     */
    public int getExpectedProcessingDays() {
        return switch (this) {
            case SUBMITTED -> 3; // 3 days to start review
            case UNDER_REVIEW -> 7; // 7 days for review
            case PENDING_DOCUMENTS -> 14; // 14 days for student to provide documents
            case APPROVED -> 7; // 7 days for student to confirm
            case CONFIRMED -> 3; // 3 days for room assignment
            case ROOM_ASSIGNED -> 1; // 1 day for move-in
            default -> 0;
        };
    }

    /**
     * Get color for UI display
     */
    public String getColor() {
        return switch (this) {
            case SUBMITTED -> "#FFA500"; // Orange
            case UNDER_REVIEW -> "#0066CC"; // Blue
            case PENDING_DOCUMENTS -> "#FF9900"; // Orange
            case APPROVED -> "#00AA00"; // Green
            case CONFIRMED -> "#006600"; // Dark Green
            case ROOM_ASSIGNED -> "#004400"; // Darker Green
            case COMPLETED -> "#002200"; // Very Dark Green
            case REJECTED -> "#CC0000"; // Red
            case WITHDRAWN -> "#999999"; // Gray
            case CANCELLED -> "#666666"; // Dark Gray
        };
    }

    /**
     * Get CSS class for styling
     */
    public String getCssClass() {
        return switch (this) {
            case SUBMITTED -> "status-submitted";
            case UNDER_REVIEW -> "status-review";
            case PENDING_DOCUMENTS -> "status-pending";
            case APPROVED -> "status-approved";
            case CONFIRMED -> "status-confirmed";
            case ROOM_ASSIGNED -> "status-assigned";
            case COMPLETED -> "status-completed";
            case REJECTED -> "status-rejected";
            case WITHDRAWN -> "status-withdrawn";
            case CANCELLED -> "status-cancelled";
        };
    }

    public static ApplicationStatus fromString(String status) {
        if (status == null)
            return SUBMITTED;

        try {
            return ApplicationStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return SUBMITTED;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}