package com.dorm.manag.entity;

/**
 * Enum representing user roles in the dormitory management system
 */
public enum Role {
    /**
     * Student - can access basic features like payments, issues reporting,
     * resident card, reservations, communication
     */
    STUDENT("Student"),

    /**
     * Admin - full system access, can manage users, monitor statistics,
     * handle all administrative tasks
     */
    ADMIN("Administrator"),

    /**
     * Receptionist - can verify resident cards, manage some user data,
     * handle reception-related tasks
     */
    RECEPTIONIST("Receptionist");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get role from string (case insensitive)
     */
    public static Role fromString(String roleStr) {
        if (roleStr == null)
            return STUDENT;

        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return STUDENT; // Default fallback
        }
    }

    /**
     * Check if role has admin privileges
     */
    public boolean hasAdminPrivileges() {
        return this == ADMIN;
    }

    /**
     * Check if role has receptionist privileges
     */
    public boolean hasReceptionistPrivileges() {
        return this == ADMIN || this == RECEPTIONIST;
    }

    /**
     * Check if role can access student features
     */
    public boolean canAccessStudentFeatures() {
        return this == STUDENT || this == ADMIN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}