package com.dorm.manag.entity;

/**
 * Enum representing types of reservable resources in the dormitory
 */
public enum ResourceType {
    /**
     * Laundry room with washing machines and dryers
     */
    LAUNDRY("Laundry Room"),

    /**
     * Game room with entertainment facilities
     */
    GAME_ROOM("Game Room"),

    /**
     * Study room for quiet learning
     */
    STUDY_ROOM("Study Room"),

    /**
     * Common kitchen for cooking
     */
    KITCHEN("Common Kitchen"),

    /**
     * Conference/meeting room
     */
    CONFERENCE_ROOM("Conference Room"),

    /**
     * Gym/fitness room
     */
    GYM("Gym"),

    /**
     * Recreation room for social activities
     */
    RECREATION_ROOM("Recreation Room"),

    /**
     * Storage space
     */
    STORAGE("Storage Space"),

    /**
     * Parking spot
     */
    PARKING("Parking Spot"),

    /**
     * Other resources
     */
    OTHER("Other");

    private final String displayName;

    ResourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get default reservation duration in minutes
     */
    public int getDefaultDurationMinutes() {
        return switch (this) {
            case LAUNDRY -> 120; // 2 hours
            case GAME_ROOM -> 60; // 1 hour
            case STUDY_ROOM -> 180; // 3 hours
            case KITCHEN -> 90; // 1.5 hours
            case CONFERENCE_ROOM -> 60; // 1 hour
            case GYM -> 90; // 1.5 hours
            case RECREATION_ROOM -> 120; // 2 hours
            case STORAGE -> 30; // 30 minutes
            case PARKING -> 1440; // 24 hours
            case OTHER -> 60; // 1 hour
        };
    }

    /**
     * Get maximum reservation duration in minutes
     */
    public int getMaxDurationMinutes() {
        return switch (this) {
            case LAUNDRY -> 240; // 4 hours max
            case GAME_ROOM -> 180; // 3 hours max
            case STUDY_ROOM -> 480; // 8 hours max
            case KITCHEN -> 180; // 3 hours max
            case CONFERENCE_ROOM -> 240; // 4 hours max
            case GYM -> 180; // 3 hours max
            case RECREATION_ROOM -> 240; // 4 hours max
            case STORAGE -> 60; // 1 hour max
            case PARKING -> 10080; // 7 days max
            case OTHER -> 120; // 2 hours max
        };
    }

    /**
     * Check if advance booking is required (hours before)
     */
    public boolean requiresAdvanceBooking() {
        return this == CONFERENCE_ROOM || this == GYM || this == RECREATION_ROOM;
    }

    /**
     * Get minimum advance booking time in hours
     */
    public int getMinAdvanceBookingHours() {
        return switch (this) {
            case CONFERENCE_ROOM -> 2;
            case GYM -> 1;
            case RECREATION_ROOM -> 1;
            default -> 0;
        };
    }

    /**
     * Check if resource requires admin approval
     */
    public boolean requiresApproval() {
        return this == CONFERENCE_ROOM || this == STORAGE;
    }

    /**
     * Get maximum reservations per user per day
     */
    public int getMaxReservationsPerDay() {
        return switch (this) {
            case LAUNDRY -> 2;
            case GAME_ROOM -> 3;
            case STUDY_ROOM -> 2;
            case KITCHEN -> 2;
            case CONFERENCE_ROOM -> 1;
            case GYM -> 2;
            case RECREATION_ROOM -> 1;
            case STORAGE -> 1;
            case PARKING -> 1;
            case OTHER -> 1;
        };
    }

    /**
     * Get cost per hour (in PLN, 0 = free) - Most dormitory resources are free
     */
    public double getCostPerHour() {
        return switch (this) {
            // All common facilities are free - users just pick up keys at reception
            case LAUNDRY, GAME_ROOM, STUDY_ROOM, KITCHEN, CONFERENCE_ROOM,
                    GYM, RECREATION_ROOM, STORAGE ->
                0.0; // FREE
            case PARKING -> 2.0; // Only parking costs money
            case OTHER -> 0.0; // Free by default
        };
    }

    public static ResourceType fromString(String type) {
        if (type == null)
            return OTHER;

        try {
            return ResourceType.valueOf(type.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}