package com.springboot.stockpilot.auth;

public class RegisterResponse {

    private final Long userId;
    private final Long customerId;
    private final String name;
    private final String email;
    private final String role;
    private final String message;

    public RegisterResponse(Long userId, Long customerId, String name, String email, String role, String message) {
        this.userId = userId;
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getMessage() {
        return message;
    }
}
