package com.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registro publico como CUSTOMER de una empresa existente.
 * El rol siempre queda en CUSTOMER: nunca llega del cliente.
 */
public class JoinRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    @NotBlank(message = "El slug de la empresa es obligatorio")
    private String tenantSlug;

    public JoinRequest() {}

    public JoinRequest(String fullName, String email, String password, String tenantSlug) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.tenantSlug = tenantSlug;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTenantSlug() { return tenantSlug; }
    public void setTenantSlug(String tenantSlug) { this.tenantSlug = tenantSlug; }
}
