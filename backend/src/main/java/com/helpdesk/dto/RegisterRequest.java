package com.helpdesk.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 2, max = 255, message = "El nombre debe tener entre 2 y 255 caracteres")
    private String fullName;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(min = 2, max = 255, message = "El nombre de la empresa debe tener entre 2 y 255 caracteres")
    private String tenantName;

    public RegisterRequest() {}

    public RegisterRequest(String fullName, String email, String password, String tenantName) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.tenantName = tenantName;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }
}
