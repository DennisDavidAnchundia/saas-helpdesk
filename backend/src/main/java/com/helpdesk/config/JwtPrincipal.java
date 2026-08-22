package com.helpdesk.config;

public class JwtPrincipal implements java.security.Principal {

    private final Long userId;
    private final Long tenantId;
    private final String email;
    private final String role;

    public JwtPrincipal(Long userId, Long tenantId, String email, String role) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.role = role;
    }

    @Override
    public String getName() { return email; }

    public Long getUserId() { return userId; }
    public Long getTenantId() { return tenantId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
}
