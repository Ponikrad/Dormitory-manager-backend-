package com.dorm.manag.entity;

/**
 * Enum representing the priority level of reported issues
 */
public enum IssuePriority {
    /**
     * Low priority - can wait for scheduled maintenance
     */
    LOW("Low", 1),

    /**
     * Medium priority - should be addressed within a few days
     */
    MEDIUM("Medium", 2),

    /**
     * High priority - should be addressed within 24 hours
     */
    HIGH("High", 3),

    /**
     * Urgent priority - requires immediate attention
     */
    URGENT("Urgent", 4),

    /**
     * Critical - emergency situation, address immediately
     */
    CRITICAL("Critical", 5);

    private final String displayName;
    private final int priorityLevel;

    IssuePriority(String displayName, int priorityLevel) {
        this.displayName = displayName;
        this.priorityLevel = priorityLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    /**
     * Check if priority requires immediate attention (within hours)
     */
    public boolean requiresImmediateAttention() {
        return this == URGENT || this == CRITICAL;
    }

    /**
     * Check if priority requires same-day response
     */
    public boolean requiresSameDayResponse() {
        return this == HIGH || this == URGENT || this == CRITICAL;
    }

    /**
     * Get maximum response time in hours
     */
    public int getMaxResponseTimeHours() {
        return switch (this) {
            case CRITICAL -> 1;
            case URGENT -> 4;
            case HIGH -> 24;
            case MEDIUM -> 72;
            case LOW -> 168; // 1 week
        };
    }

    /**
     * Get recommended resolution time in hours
     */
    public int getRecommendedResolutionTimeHours() {
        return switch (this) {
            case CRITICAL -> 2;
            case URGENT -> 8;
            case HIGH -> 48;
            case MEDIUM -> 120; // 5 days
            case LOW -> 240; // 10 days
        };
    }

    /**
     * Auto-determine priority based on category
     */
    public static IssuePriority determinePriority(IssueCategory category, String description) {
        // Emergency keywords
        if (description != null) {
            String desc = description.toLowerCase();
            if (desc.contains("emergency") || desc.contains("danger") || desc.contains("urgent") ||
                    desc.contains("flooding") || desc.contains("fire") || desc.contains("gas leak")) {
                return CRITICAL;
            }
            if (desc.contains("urgent") || desc.contains("asap") || desc.contains("immediately")) {
                return URGENT;
            }
        }

        // Category-based priority
        return switch (category) {
            case PLUMBING -> description != null && description.toLowerCase().contains("leak") ? URGENT : HIGH;
            case ELECTRICAL -> URGENT;
            case SECURITY -> URGENT;
            case HEATING -> HIGH;
            case INTERNET -> MEDIUM;
            case CLEANING -> LOW;
            case FURNITURE -> MEDIUM;
            case KITCHEN -> MEDIUM;
            case BATHROOM -> HIGH;
            case NOISE -> MEDIUM;
            case OTHER -> MEDIUM;
        };
    }

    /**
     * Get CSS class for priority styling
     */
    public String getCssClass() {
        return switch (this) {
            case CRITICAL -> "priority-critical";
            case URGENT -> "priority-urgent";
            case HIGH -> "priority-high";
            case MEDIUM -> "priority-medium";
            case LOW -> "priority-low";
        };
    }

    /**
     * Get color for priority display
     */
    public String getColor() {
        return switch (this) {
            case CRITICAL -> "#FF0000"; // Red
            case URGENT -> "#FF6600"; // Orange-Red
            case HIGH -> "#FF9900"; // Orange
            case MEDIUM -> "#FFCC00"; // Yellow
            case LOW -> "#00CC00"; // Green
        };
    }

    /**
     * Compare priorities (higher priority level = more important)
     */
    public boolean isHigherThan(IssuePriority other) {
        return this.priorityLevel > other.priorityLevel;
    }

    /**
     * Get priority from string
     */
    public static IssuePriority fromString(String priority) {
        if (priority == null)
            return MEDIUM;

        try {
            return IssuePriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM; // Default fallback
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}