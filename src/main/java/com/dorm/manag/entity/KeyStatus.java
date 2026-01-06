package com.dorm.manag.entity;

/**
 * Enum representing the status of keys in the dormitory system
 */
public enum KeyStatus {
    /**
     * Key is available for assignment
     */
    AVAILABLE("Available"),

    /**
     * Key is currently issued to a user
     */
    ISSUED("Issued"),

    /**
     * Key has been reported as lost
     */
    LOST("Lost"),

    /**
     * Key has been damaged and needs replacement
     */
    DAMAGED("Damaged"),

    /**
     * Key is out of service for maintenance/replacement
     */
    OUT_OF_SERVICE("Out of Service"),

    /**
     * Key has been permanently retired
     */
    RETIRED("Retired"),

    /**
     * Key is reserved for specific user/purpose
     */
    RESERVED("Reserved");

    private final String displayName;

    KeyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if key can be issued in current status
     */
    public boolean canBeIssued() {
        return this == AVAILABLE || this == RESERVED;
    }

    /**
     * Check if key is currently with a user
     */
    public boolean isWithUser() {
        return this == ISSUED;
    }

    /**
     * Check if key needs attention/action
     */
    public boolean needsAttention() {
        return this == LOST || this == DAMAGED || this == OUT_OF_SERVICE;
    }

    /**
     * Check if key is in final state
     */
    public boolean isFinal() {
        return this == RETIRED;
    }

    /**
     * Get next possible statuses
     */
    public KeyStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case AVAILABLE -> new KeyStatus[] { ISSUED, RESERVED, DAMAGED, OUT_OF_SERVICE };
            case ISSUED -> new KeyStatus[] { AVAILABLE, LOST, DAMAGED };
            case LOST -> new KeyStatus[] { AVAILABLE, OUT_OF_SERVICE, RETIRED };
            case DAMAGED -> new KeyStatus[] { AVAILABLE, OUT_OF_SERVICE, RETIRED };
            case OUT_OF_SERVICE -> new KeyStatus[] { AVAILABLE, RETIRED };
            case RESERVED -> new KeyStatus[] { ISSUED, AVAILABLE };
            case RETIRED -> new KeyStatus[] {}; // Final state
        };
    }

    /**
     * Get color for UI display
     */
    public String getColor() {
        return switch (this) {
            case AVAILABLE -> "#00AA00"; // Green
            case ISSUED -> "#0066CC"; // Blue
            case RESERVED -> "#FF9900"; // Orange
            case LOST -> "#CC0000"; // Red
            case DAMAGED -> "#FF6600"; // Orange-Red
            case OUT_OF_SERVICE -> "#999999"; // Gray
            case RETIRED -> "#666666"; // Dark Gray
        };
    }

    /**
     * Get CSS class for styling
     */
    public String getCssClass() {
        return switch (this) {
            case AVAILABLE -> "key-available";
            case ISSUED -> "key-issued";
            case RESERVED -> "key-reserved";
            case LOST -> "key-lost";
            case DAMAGED -> "key-damaged";
            case OUT_OF_SERVICE -> "key-out-of-service";
            case RETIRED -> "key-retired";
        };
    }

    public static KeyStatus fromString(String status) {
        if (status == null)
            return AVAILABLE;

        try {
            return KeyStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return AVAILABLE;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}