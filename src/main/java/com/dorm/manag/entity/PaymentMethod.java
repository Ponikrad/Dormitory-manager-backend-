package com.dorm.manag.entity;

/**
 * Enum representing available payment methods in the dormitory system
 */
public enum PaymentMethod {
    /**
     * Credit/Debit card payment
     */
    CARD("Card Payment"),

    /**
     * BLIK mobile payment (popular in Poland)
     */
    BLIK("BLIK Payment"),

    /**
     * Bank transfer
     */
    BANK_TRANSFER("Bank Transfer"),

    /**
     * Cash payment at reception
     */
    CASH("Cash Payment"),

    /**
     * Online payment gateway (PayU, Stripe, etc.)
     */
    ONLINE("Online Payment");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if payment method requires online processing
     */
    public boolean requiresOnlineProcessing() {
        return this == CARD || this == BLIK || this == ONLINE;
    }

    /**
     * Check if payment method is instant
     */
    public boolean isInstant() {
        return this == CARD || this == BLIK || this == CASH;
    }

    /**
     * Get payment method from string
     */
    public static PaymentMethod fromString(String method) {
        if (method == null)
            return CARD;

        try {
            return PaymentMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CARD; // Default fallback
        }
    }

    @Override
    public String toString() {
        return displayName;
    }
}