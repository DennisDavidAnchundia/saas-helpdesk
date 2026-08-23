package com.helpdesk.dto;

import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;

import java.time.LocalDateTime;

/** Version publica de un usuario del tenant para el panel admin. */
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private boolean active;
    private String provider;
    private LocalDateTime createdAt;

    public UserResponse() {}

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.fullName = u.getFullName();
        r.email = u.getEmail();
        r.role = u.getRole();
        r.active = u.isActive();
        r.provider = u.getProvider() != null ? u.getProvider().name() : null;
        r.createdAt = u.getCreatedAt();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
