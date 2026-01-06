package com.dorm.manag.entity;

/**
 * Enum representing types of keys in the dormitory system
 */
public enum KeyType {
    /**
     * Room key for student rooms
     */
    ROOM("Room Key"),

    /**
     * Laundry room key
     */
    LAUNDRY("Laundry Room Key"),

    /**
     * Game room key
     */
    GAME_ROOM("Game Room Key"),

    /**
     * Study room key
     */
    STUDY_ROOM("Study Room Key"),

    /**
     * Kitchen key
     */
    KITCHEN("Kitchen Key"),

    /**
     * Gym key
     */
    GYM("Gym Key"),

    /**
     * Storage room key
     */
    STORAGE("Storage Key"),

    /**
     * Building main entrance key
     */
    BUILDING_ENTRANCE("Building Entrance Key"),

    /**
     * Floor access key
     */
    FLOOR_ACCESS("Floor Access Key"),

    /**
     * Master key (admin only)
     */
    MASTER("Master Key"),

    /**
     * Other specialized keys
     */
    OTHER("Other Key");

    private final String displayName;

    KeyType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if key type requires admin authorization
     */
    public boolean requiresAdminAuthorization() {
        return this == MASTER || this == BUILDING_ENTRANCE || this == FLOOR_ACCESS;
    }

    /**
     * Check if key can be temporarily issued (for reservations)
     */
    public boolean canBeTemporaryIssued() {
        return this == LAUNDRY || this == GAME_ROOM || this == STUDY_ROOM ||
                this == KITCHEN || this == GYM || this == STORAGE;
    }

    /**
     * Check if key is for permanent assignment
     */
    public boolean isPermanentAssignment() {
        return this == ROOM;
    }

    /**
     * Get default maximum issue duration in hours
     */
    public int getMaxIssueDurationHours() {
        return switch (this) {
            case ROOM -> 8760; // 1 year for room keys
            case LAUNDRY -> 4; // 4 hours for laundry
            case GAME_ROOM, STUDY_ROOM -> 8; // 8 hours
            case KITCHEN -> 3; // 3 hours
            case GYM -> 2; // 2 hours
            case STORAGE -> 1; // 1 hour
            case BUILDING_ENTRANCE, FLOOR_ACCESS -> 24; // 24 hours
            case MASTER -> 8; // 8 hours max for master keys
            case OTHER -> 4; // 4 hours default
        };
    }

    /**
     * Get security level (1-5, 5 being highest)
     */
    public int getSecurityLevel() {
        return switch (this) {
            case MASTER -> 5;
            case BUILDING_ENTRANCE -> 4;
            case FLOOR_ACCESS -> 3;
            case ROOM -> 3;
            case STORAGE -> 2;
            case LAUNDRY, GAME_ROOM, STUDY_ROOM, KITCHEN, GYM -> 1;
            case OTHER -> 2;
        };
    }

    /**
     * Check if key requires deposit
     */
    public boolean requiresDeposit() {
        return getSecurityLevel() >= 3;
    }

    /**
     * Get deposit amount in PLN
     */
    public double getDepositAmount() {
        return switch (this) {
            case MASTER -> 500.0;
            case BUILDING_ENTRANCE -> 200.0;
            case ROOM, FLOOR_ACCESS -> 100.0;
            case STORAGE -> 50.0;
            default -> 0.0;
        };
    }

    public static KeyType fromString(String type) {
        if (type == null)
            return OTHER;

        try {
            return KeyType.valueOf(type.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}