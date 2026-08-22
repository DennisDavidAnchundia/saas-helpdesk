package com.helpdesk.dto;

import java.time.LocalDateTime;

public class RegisterResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Long tenantId;
    private String tenantName;
    private String tenantSlug;
    private LocalDateTime createdAt;

    public RegisterResponse() {}

    public RegisterResponse(Long userId, String email, String fullName, String role,
                            Long tenantId, String tenantName, String tenantSlug, LocalDateTime createdAt) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.tenantSlug = tenantSlug;
        this.createdAt = createdAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getTenantSlug() { return tenantSlug; }
    public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
