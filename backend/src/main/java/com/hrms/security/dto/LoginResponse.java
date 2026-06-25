package com.hrms.security.dto;

/**
 * Result of a successful login: the bearer token, its lifetime, and a summary
 * of the authenticated user (including resolved authorities for the UI).
 */
public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresInMinutes;
    private UserDto user;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiresInMinutes, UserDto user) {
        this.token = token;
        this.expiresInMinutes = expiresInMinutes;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresInMinutes() { return expiresInMinutes; }
    public void setExpiresInMinutes(long expiresInMinutes) { this.expiresInMinutes = expiresInMinutes; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
}
