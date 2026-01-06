package com.dorm.manag.entity;

/**
 * Enum representing types of messages in the communication system
 */
public enum MessageType {
    /**
     * Direct message between user and admin/reception
     */
    DIRECT("Direct Message"),

    /**
     * Reply to a direct message
     */
    REPLY("Reply"),

    /**
     * Question or inquiry
     */
    INQUIRY("Inquiry"),

    /**
     * Complaint about facilities or services
     */
    COMPLAINT("Complaint"),

    /**
     * Request for assistance or service
     */
    REQUEST("Service Request"),

    /**
     * Maintenance-related communication
     */
    MAINTENANCE("Maintenance"),

    /**
     * Payment-related inquiry
     */
    PAYMENT("Payment Inquiry"),

    /**
     * Reservation-related communication
     */
    RESERVATION("Reservation"),

    /**
     * General information request
     */
    INFORMATION("Information"),

    /**
     * Other type of message
     */
    OTHER("Other");

    private final String displayName;

    MessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if message type requires urgent response
     */
    public boolean requiresUrgentResponse() {
        return this == COMPLAINT || this == MAINTENANCE;
    }

    /**
     * Get expected response time in hours
     */
    public int getExpectedResponseTimeHours() {
        return switch (this) {
            case COMPLAINT, MAINTENANCE -> 4; // 4 hours for urgent
            case REQUEST, PAYMENT, RESERVATION -> 24; // 24 hours for important
            case INQUIRY, INFORMATION -> 48; // 48 hours for general
            case DIRECT, REPLY, OTHER -> 72; // 72 hours for others
        };
    }

    /**
     * Check if message should be routed to specific department
     */
    public String getRoutingDepartment() {
        return switch (this) {
            case MAINTENANCE -> "MAINTENANCE";
            case PAYMENT -> "FINANCE";
            case RESERVATION -> "RECEPTION";
            case COMPLAINT -> "ADMIN";
            default -> "GENERAL";
        };
    }

    /**
     * Get priority level (1-5, 5 being highest)
     */
    public int getPriority() {
        return switch (this) {
            case COMPLAINT, MAINTENANCE -> 5;
            case REQUEST, PAYMENT -> 4;
            case RESERVATION -> 3;
            case INQUIRY, INFORMATION -> 2;
            case DIRECT, REPLY, OTHER -> 1;
        };
    }

    public static MessageType fromString(String type) {
        if (type == null)
            return OTHER;

        try {
            return MessageType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}