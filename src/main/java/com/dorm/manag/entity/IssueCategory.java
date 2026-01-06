package com.dorm.manag.entity;

/**
 * Enum representing categories of issues that can be reported in the dormitory
 */
public enum IssueCategory {
    /**
     * Plumbing issues (leaks, broken pipes, no hot water, etc.)
     */
    PLUMBING("Plumbing"),

    /**
     * Electrical problems (power outages, broken outlets, lighting, etc.)
     */
    ELECTRICAL("Electrical"),

    /**
     * Heating and cooling issues (radiators, AC, ventilation, etc.)
     */
    HEATING("Heating & Cooling"),

    /**
     * Cleaning and maintenance issues (dirty common areas, garbage, etc.)
     */
    CLEANING("Cleaning & Maintenance"),

    /**
     * Furniture problems (broken bed, desk, chair, etc.)
     */
    FURNITURE("Furniture"),

    /**
     * Internet and network connectivity issues
     */
    INTERNET("Internet & Network"),

    /**
     * Security issues (broken locks, windows, safety concerns)
     */
    SECURITY("Security"),

    /**
     * Kitchen appliances and equipment
     */
    KITCHEN("Kitchen Equipment"),

    /**
     * Noise complaints and disturbances
     */
    NOISE("Noise Issues"),

    /**
     * Bathroom facilities issues
     */
    BATHROOM("Bathroom"),

    /**
     * Other issues that don't fit into above categories
     */
    OTHER("Other");

    private final String displayName;

    IssueCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if issue requires immediate attention
     */
    public boolean requiresImmediateAttention() {
        return this == PLUMBING || this == ELECTRICAL || this == SECURITY || this == HEATING;
    }

    /**
     * Check if issue can be resolved by cleaning staff
     */
    public boolean canBeResolvedByCleaningStaff() {
        return this == CLEANING;
    }

    /**
     * Check if issue requires external contractor
     */
    public boolean requiresExternalContractor() {
        return this == PLUMBING || this == ELECTRICAL || this == HEATING || this == INTERNET;
    }

    /**
     * Get estimated resolution time in hours
     */
    public int getEstimatedResolutionHours() {
        return switch (this) {
            case PLUMBING, ELECTRICAL, SECURITY -> 4; // Urgent issues
            case HEATING, INTERNET -> 8;
            case CLEANING -> 2;
            case FURNITURE, KITCHEN, BATHROOM -> 24;
            case NOISE -> 1;
            case OTHER -> 48;
        };
    }

    /**
     * Get category from string
     */
    public static IssueCategory fromString(String category) {
        if (category == null)
            return OTHER;

        try {
            return IssueCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER; // Default fallback
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}