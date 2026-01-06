package com.dorm.manag.entity;

/**
 * Enum representing payment status in the system
 */
public enum PaymentStatus {
    /**
     * Payment has been created but not yet processed
     */
    PENDING("Pending"),

    /**
     * Payment is currently being processed
     */
    PROCESSING("Processing"),

    /**
     * Payment completed successfully
     */
    COMPLETED("Completed"),

    /**
     * Payment failed due to technical or financial reasons
     */
    FAILED("Failed"),

    /**
     * Payment was cancelled by user or system
     */
    CANCELLED("Cancelled"),

    /**
     * Payment was refunded to the user
     */
    REFUNDED("Refunded"),

    /**
     * Payment expired (e.g., bank transfer not made within time limit)
     */
    EXPIRED("Expired");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if payment is in final state (cannot be changed)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED || this == REFUNDED || this == EXPIRED;
    }

    /**
     * Check if payment is successful
     */
    public boolean isSuccessful() {
        return this == COMPLETED;
    }

    /**
     * Check if payment can be cancelled
     */
    public boolean canBeCancelled() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Check if payment can be refunded
     */
    public boolean canBeRefunded() {
        return this == COMPLETED;
    }

    /**
     * Get next possible statuses from current status
     */
    public PaymentStatus[] getNextPossibleStatuses() {
        return switch (this) {
            case PENDING -> new PaymentStatus[] { PROCESSING, CANCELLED, EXPIRED };
            case PROCESSING -> new PaymentStatus[] { COMPLETED, FAILED, CANCELLED };
            case COMPLETED -> new PaymentStatus[] { REFUNDED };
            case FAILED, CANCELLED, REFUNDED, EXPIRED -> new PaymentStatus[] {};
        };
    }

    /**
     * Get status from string
     */
    public static PaymentStatus fromString(String status) {
        if (status == null)
            return PENDING;

        try {
            return PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PENDING; // Default fallback
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}