package com.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateCheckoutRequest {

    @NotBlank
    @Pattern(regexp = "PRO|ENTERPRISE", message = "El plan debe ser PRO o ENTERPRISE")
    private String targetPlan;

    public String getTargetPlan() { return targetPlan; }
    public void setTargetPlan(String targetPlan) { this.targetPlan = targetPlan; }
}
