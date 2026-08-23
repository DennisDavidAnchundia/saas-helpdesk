package com.helpdesk.dto;

import com.helpdesk.model.User;

/** Version publica de un agente para selects de asignacion (sin datos sensibles). */
public class AgentResponse {

    private Long id;
    private String fullName;
    private String email;

    public AgentResponse() {}

    public static AgentResponse from(User u) {
        AgentResponse r = new AgentResponse();
        r.id = u.getId();
        r.fullName = u.getFullName();
        r.email = u.getEmail();
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
