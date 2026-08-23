package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.SlaPolicyResponse;
import com.helpdesk.dto.UpdateSlaPolicyRequest;
import com.helpdesk.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/sla")
    @PreAuthorize("hasRole('ADMIN')")
    public SlaPolicyResponse getSlaPolicy(@AuthenticationPrincipal JwtPrincipal principal) {
        return tenantService.getSlaPolicy(principal.getTenantId());
    }

    @PutMapping("/sla")
    @PreAuthorize("hasRole('ADMIN')")
    public SlaPolicyResponse updateSlaPolicy(@Valid @RequestBody UpdateSlaPolicyRequest request,
                                             @AuthenticationPrincipal JwtPrincipal principal) {
        return tenantService.updateSlaPolicy(principal.getTenantId(), request);
    }
}
