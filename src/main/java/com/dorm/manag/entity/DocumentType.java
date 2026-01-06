package com.dorm.manag.entity;

/**
 * Enum representing types of documents in the dormitory system
 */
public enum DocumentType {
    /**
     * Dormitory regulations and rules
     */
    REGULATIONS("Regulations"),

    /**
     * General information documents
     */
    INFORMATION("Information"),

    /**
     * Forms for students to fill out
     */
    FORM("Form"),

    /**
     * Instructions and guides
     */
    GUIDE("Guide"),

    /**
     * Legal documents and contracts
     */
    LEGAL("Legal Document"),

    /**
     * Emergency procedures and contacts
     */
    EMERGENCY("Emergency Information"),

    /**
     * Maps and floor plans
     */
    MAP("Map"),

    /**
     * Price lists and fee schedules
     */
    PRICE_LIST("Price List"),

    /**
     * Event information and schedules
     */
    EVENT_INFO("Event Information"),

    /**
     * Contact information and directories
     */
    CONTACT("Contact Information"),

    /**
     * Other document types
     */
    OTHER("Other");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if document type requires approval for changes
     */
    public boolean requiresApproval() {
        return this == REGULATIONS || this == LEGAL || this == EMERGENCY;
    }

    /**
     * Check if document should be prominently displayed
     */
    public boolean isImportant() {
        return this == REGULATIONS || this == EMERGENCY || this == MAP;
    }

    /**
     * Get default access level
     */
    public String getDefaultAccessLevel() {
        return switch (this) {
            case LEGAL, REGULATIONS -> "PUBLIC"; // Everyone should see these
            case EMERGENCY, MAP, CONTACT -> "PUBLIC";
            case FORM, GUIDE, INFORMATION -> "STUDENTS";
            case PRICE_LIST, EVENT_INFO -> "PUBLIC";
            case OTHER -> "ADMIN";
        };
    }

    /**
     * Check if document type supports versioning
     */
    public boolean supportsVersioning() {
        return this == REGULATIONS || this == LEGAL || this == PRICE_LIST || this == GUIDE;
    }

    public static DocumentType fromString(String type) {
        if (type == null)
            return OTHER;

        try {
            return DocumentType.valueOf(type.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}