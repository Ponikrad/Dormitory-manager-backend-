package com.dorm.manag.entity;

/**
 * Enum representing the status of a reservation
 */
public enum ReservationStatus {
    /**
     * Reservation has been created and is pending approval (if required)
     */
    PENDING("Pending"),

    /**
     * Reservation has been confirmed and is active
     */
    CONFIRMED("Confirmed"),

    /**
     * User has checked in and is currently using the resource
     */
    CHECKED_IN("Checked In"),

    /**
     * Reservation has been completed successfully
     */
    COMPLETED("Completed"),

    /**
     * Reservation was cancelled by user
     */
    CANCELLED("Cancelled"),

    /**
     * Reservation was rejected (e.g., by admin)
     */
    REJECTED("Rejected"),

    /**
     * User didn't show up for the reservation
     */
    NO_SHOW("No Show"),

    /**
     * Reservation has expired (past end time without completion)
     */
    EXPIRED("Expired");

    private final String displayName;

    ReservationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if reservation is active (user can use the resource)
     */
    public boolean isActive() {
        return this == CONFIRMED || this == CHECKED_IN;
    }

    /**
     * Check if reservation is final (cannot be modified)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == REJECTED ||
                this == NO_SHOW || this == EXPIRED;
    }

    /**
     * Check if reservation can be cancelled by user
     */
    public boolean canBeCancelled() {
        return this == PENDING || this == CONFIRMED;
    }

    /**
     * Check if user can check in
     */
    public boolean canCheckIn() {
        return this == CONFIRMED;
    }

    /**
     * Check if reservation requires payment
     */
    public boolean requiresPayment() {
        return this == CONFIRMED || this == CHECKED_IN || this == COMPLETED;
    }

    /**
     * Get next possible statuses
     */
    public ReservationStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case PENDING -> new ReservationStatus[] { CONFIRMED, REJECTED, CANCELLED };
            case CONFIRMED -> new ReservationStatus[] { CHECKED_IN, CANCELLED, NO_SHOW, EXPIRED };
            case CHECKED_IN -> new ReservationStatus[] { COMPLETED, EXPIRED };
            case COMPLETED, CANCELLED, REJECTED, NO_SHOW, EXPIRED -> new ReservationStatus[] {};
        };
    }

    /**
     * Check if status transition is valid
     */
    public boolean canTransitionTo(ReservationStatus newStatus) {
        ReservationStatus[] allowed = getNextPossibleStatuses();
        for (ReservationStatus status : allowed) {
            if (status == newStatus)
                return true;
        }
        return false;
    }

    /**
     * Get CSS class for UI styling
     */
    public String getCssClass() {
        return switch (this) {
            case PENDING -> "status-pending";
            case CONFIRMED -> "status-confirmed";
            case CHECKED_IN -> "status-active";
            case COMPLETED -> "status-completed";
            case CANCELLED -> "status-cancelled";
            case REJECTED -> "status-rejected";
            case NO_SHOW -> "status-no-show";
            case EXPIRED -> "status-expired";
        };
    }

    /**
     * Get color for display
     */
    public String getColor() {
        return switch (this) {
            case PENDING -> "#FFA500"; // Orange
            case CONFIRMED -> "#00AA00"; // Green
            case CHECKED_IN -> "#0066CC"; // Blue
            case COMPLETED -> "#006600"; // Dark Green
            case CANCELLED -> "#999999"; // Gray
            case REJECTED -> "#CC0000"; // Red
            case NO_SHOW -> "#FF6600"; // Orange-Red
            case EXPIRED -> "#666666"; // Dark Gray
        };
    }

    public static ReservationStatus fromString(String status) {
        if (status == null)
            return PENDING;

        try {
            return ReservationStatus.valueOf(status.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}