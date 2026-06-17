package com.civicdesk.module.iam.dto.response;

/** Payload returned on successful citizen / staff login. */
public class AuthResponse {

    private String token;
    private String userId;
    private String role;
    private long expiresIn;

    public AuthResponse() {
    }

    public AuthResponse(String token, String userId, String role, long expiresIn) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
