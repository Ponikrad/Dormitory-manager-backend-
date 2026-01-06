package com.dorm.manag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    public static LoginRequest of(String username, String password) {
        return new LoginRequest(username, password);
    }

    public boolean isValid() {
        return username != null && !username.trim().isEmpty()
                && password != null && !password.trim().isEmpty()
                && username.length() >= 3 && username.length() <= 50
                && password.length() >= 6 && password.length() <= 100;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}