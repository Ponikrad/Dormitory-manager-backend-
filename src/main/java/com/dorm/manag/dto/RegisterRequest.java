package com.dorm.manag.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, numbers, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @JsonProperty("first_name")
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @JsonProperty("last_name")
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @JsonProperty("phone_number")
    @Pattern(regexp = "^[+]?[0-9]{9,15}$", message = "Please provide a valid phone number")
    private String phoneNumber;

    @JsonProperty("room_number")
    @Size(max = 20, message = "Room number must not exceed 20 characters")
    private String roomNumber;

    public static RegisterRequest of(String username, String email, String password,
            String firstName, String lastName) {
        return new RegisterRequest(username, email, password, firstName, lastName, null, null);
    }

    public boolean hasValidPassword() {
        return password != null && password.length() >= 6 &&
                password.matches(".*[A-Za-z].*") &&
                password.matches(".*[0-9].*");
    }

    public boolean hasValidEmail() {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public boolean hasValidPhoneNumber() {
        return phoneNumber == null || phoneNumber.matches("^[+]?[0-9]{9,15}$");
    }

    public boolean isValid() {
        return username != null && !username.trim().isEmpty() &&
                email != null && hasValidEmail() &&
                password != null && password.length() >= 6 &&
                firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty() &&
                hasValidPhoneNumber();
    }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", roomNumber='" + roomNumber + '\'' +
                '}';
    }
}