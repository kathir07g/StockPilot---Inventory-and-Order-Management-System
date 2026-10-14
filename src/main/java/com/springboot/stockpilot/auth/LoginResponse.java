package com.springboot.stockpilot.auth;

public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final Long userId;
    private final String name;
    private final String email;
    private final String role;

    public LoginResponse(String accessToken, long expiresIn,
                         Long userId, String name, String email, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public Long getUserId() {
        return userId;
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
}
