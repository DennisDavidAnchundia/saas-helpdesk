package com.helpdesk.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateUserActiveRequest {

    @NotNull(message = "isActive es obligatorio")
    private Boolean isActive;

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
