package com.dorm.manag.entity;

/**
 * Enum representing the status of messages in the communication system
 */
public enum MessageStatus {
    /**
     * Message has been sent but not yet delivered/read
     */
    SENT("Sent"),

    /**
     * Message has been delivered to recipient
     */
    DELIVERED("Delivered"),

    /**
     * Message has been read by recipient
     */
    READ("Read"),

    /**
     * Message has been replied to
     */
    REPLIED("Replied"),

    /**
     * Message thread has been resolved/closed
     */
    RESOLVED("Resolved"),

    /**
     * Message was archived
     */
    ARCHIVED("Archived");

    private final String displayName;

    MessageStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if message is in final state
     */
    public boolean isFinal() {
        return this == RESOLVED || this == ARCHIVED;
    }

    /**
     * Check if message needs response
     */
    public boolean needsResponse() {
        return this == DELIVERED || this == READ;
    }

    /**
     * Get next possible statuses
     */
    public MessageStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case SENT -> new MessageStatus[] { DELIVERED, ARCHIVED };
            case DELIVERED -> new MessageStatus[] { READ, REPLIED, ARCHIVED };
            case READ -> new MessageStatus[] { REPLIED, RESOLVED, ARCHIVED };
            case REPLIED -> new MessageStatus[] { RESOLVED, ARCHIVED };
            case RESOLVED -> new MessageStatus[] { ARCHIVED };
            case ARCHIVED -> new MessageStatus[] {}; // Final state
        };
    }

    /**
     * Get CSS class for UI styling
     */
    public String getCssClass() {
        return switch (this) {
            case SENT -> "status-sent";
            case DELIVERED -> "status-delivered";
            case READ -> "status-read";
            case REPLIED -> "status-replied";
            case RESOLVED -> "status-resolved";
            case ARCHIVED -> "status-archived";
        };
    }

    /**
     * Get color for display
     */
    public String getColor() {
        return switch (this) {
            case SENT -> "#FFA500"; // Orange
            case DELIVERED -> "#FFD700"; // Gold
            case READ -> "#0066CC"; // Blue
            case REPLIED -> "#00AA00"; // Green
            case RESOLVED -> "#006600"; // Dark Green
            case ARCHIVED -> "#999999"; // Gray
        };
    }

    public static MessageStatus fromString(String status) {
        if (status == null)
            return SENT;

        try {
            return MessageStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SENT;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}