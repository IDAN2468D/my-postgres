package com.idan.postgresapp.dto;

import java.util.Map;

public class AuthResponse {
    private String status;
    private String message;
    private String token;
    private String tokenType = "Bearer";
    private Map<String, Object> user;

    public AuthResponse() {}

    public AuthResponse(String status, String message, String token, Map<String, Object> user) {
        this.status = status;
        this.message = message;
        this.token = token;
        this.user = user;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public Map<String, Object> getUser() { return user; }
    public void setUser(Map<String, Object> user) { this.user = user; }
}
