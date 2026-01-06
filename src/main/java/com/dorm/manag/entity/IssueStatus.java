package com.dorm.manag.entity;

/**
 * Enum representing the status of reported issues
 */
public enum IssueStatus {
    /**
     * Issue has been reported but not yet reviewed
     */
    REPORTED("Reported"),

    /**
     * Issue has been acknowledged by staff
     */
    ACKNOWLEDGED("Acknowledged"),

    /**
     * Issue is currently being worked on
     */
    IN_PROGRESS("In Progress"),

    /**
     * Issue is waiting for parts or external contractor
     */
    WAITING_FOR_PARTS("Waiting for Parts"),

    /**
     * Issue has been escalated to higher priority
     */
    ESCALATED("Escalated"),

    /**
     * Issue has been resolved successfully
     */
    RESOLVED("Resolved"),

    /**
     * Issue was cancelled (duplicate, invalid, etc.)
     */
    CANCELLED("Cancelled"),

    /**
     * Issue was closed without resolution (e.g., user withdrawn)
     */
    CLOSED("Closed");

    private final String displayName;

    IssueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if issue is in active state (being worked on)
     */
    public boolean isActive() {
        return this == ACKNOWLEDGED || this == IN_PROGRESS || this == WAITING_FOR_PARTS || this == ESCALATED;
    }

    /**
     * Check if issue is closed (final state)
     */
    public boolean isClosed() {
        return this == RESOLVED || this == CANCELLED || this == CLOSED;
    }

    /**
     * Check if issue can be reopened
     */
    public boolean canBeReopened() {
        return this == RESOLVED || this == CLOSED;
    }

    /**
     * Check if issue can be cancelled
     */
    public boolean canBeCancelled() {
        return !isClosed();
    }

    /**
     * Check if issue requires admin attention
     */
    public boolean requiresAdminAttention() {
        return this == ESCALATED || this == REPORTED;
    }

    /**
     * Get next possible statuses from current status
     */
    public IssueStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case REPORTED -> new IssueStatus[] { ACKNOWLEDGED, CANCELLED, ESCALATED };
            case ACKNOWLEDGED -> new IssueStatus[] { IN_PROGRESS, WAITING_FOR_PARTS, ESCALATED, CANCELLED };
            case IN_PROGRESS -> new IssueStatus[] { WAITING_FOR_PARTS, RESOLVED, ESCALATED };
            case WAITING_FOR_PARTS -> new IssueStatus[] { IN_PROGRESS, RESOLVED, ESCALATED };
            case ESCALATED -> new IssueStatus[] { IN_PROGRESS, RESOLVED, CANCELLED };
            case RESOLVED -> new IssueStatus[] { REPORTED, CLOSED }; // Can reopen or close
            case CANCELLED, CLOSED -> new IssueStatus[] { REPORTED }; // Can reopen
        };
    }

    /**
     * Get CSS class for status styling
     */
    public String getCssClass() {
        return switch (this) {
            case REPORTED -> "status-new";
            case ACKNOWLEDGED -> "status-acknowledged";
            case IN_PROGRESS -> "status-progress";
            case WAITING_FOR_PARTS -> "status-waiting";
            case ESCALATED -> "status-escalated";
            case RESOLVED -> "status-resolved";
            case CANCELLED -> "status-cancelled";
            case CLOSED -> "status-closed";
        };
    }

    /**
     * Get status from string
     */
    public static IssueStatus fromString(String status) {
        if (status == null)
            return REPORTED;

        try {
            return IssueStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return REPORTED; // Default fallback
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}