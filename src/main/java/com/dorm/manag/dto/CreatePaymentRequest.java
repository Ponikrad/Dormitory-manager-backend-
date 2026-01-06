package com.dorm.manag.dto;

import com.dorm.manag.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "10000.00", message = "Amount cannot exceed 10,000")
    @Digits(integer = 8, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 255, message = "Description must be between 5 and 255 characters")
    private String description;

    @Size(max = 50, message = "Payment type cannot exceed 50 characters")
    private String paymentType;

    private LocalDateTime dueDate;

    private LocalDateTime periodStart;

    private LocalDateTime periodEnd;

    @Size(max = 3, message = "Currency code must be 3 characters")
    private String currency = "PLN";

    public boolean isValidAmount() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 &&
                amount.compareTo(new BigDecimal("10000.00")) <= 0;
    }

    public boolean isValidDueDate() {
        return dueDate == null || dueDate.isAfter(LocalDateTime.now());
    }

    public boolean isValidPeriod() {
        if (periodStart == null || periodEnd == null)
            return true;
        return periodStart.isBefore(periodEnd);
    }

    public boolean isValid() {
        return isValidAmount() &&
                paymentMethod != null &&
                description != null && !description.trim().isEmpty() &&
                isValidDueDate() &&
                isValidPeriod();
    }

    public String getFormattedAmount() {
        return amount != null ? String.format("%.2f %s", amount, currency) : "0.00 PLN";
    }

    public boolean isRecurringPayment() {
        return periodStart != null && periodEnd != null;
    }

    public static CreatePaymentRequest forRent(BigDecimal amount, String roomNumber) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(amount);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setDescription("Rent payment for room " + roomNumber);
        request.setPaymentType("RENT");
        request.setDueDate(LocalDateTime.now().plusDays(30));
        return request;
    }

    public static CreatePaymentRequest forUtilities(BigDecimal amount, String roomNumber) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setAmount(amount);
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setDescription("Utilities payment for room " + roomNumber);
        request.setPaymentType("UTILITIES");
        request.setDueDate(LocalDateTime.now().plusDays(15));
        return request;
    }

    @Override
    public String toString() {
        return "CreatePaymentRequest{" +
                "amount=" + amount +
                ", paymentMethod=" + paymentMethod +
                ", description='" + description + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", dueDate=" + dueDate +
                ", currency='" + currency + '\'' +
                '}';
    }
}