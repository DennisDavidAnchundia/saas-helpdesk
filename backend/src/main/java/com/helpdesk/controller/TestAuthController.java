package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestAuthController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminEndpoint(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "message", "Hola Admin!",
                "userId", principal.getUserId(),
                "tenantId", principal.getTenantId(),
                "email", principal.getEmail(),
                "role", principal.getRole()
        ));
    }

    @GetMapping("/agent")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<Map<String, Object>> agentEndpoint(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "message", "Hola Agent!",
                "userId", principal.getUserId(),
                "tenantId", principal.getTenantId(),
                "email", principal.getEmail(),
                "role", principal.getRole()
        ));
    }

    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> customerEndpoint(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "message", "Hola Customer!",
                "userId", principal.getUserId(),
                "tenantId", principal.getTenantId(),
                "email", principal.getEmail(),
                "role", principal.getRole()
        ));
    }

    @GetMapping("/any")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'CUSTOMER')")
    public ResponseEntity<Map<String, Object>> anyRoleEndpoint(@AuthenticationPrincipal JwtPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "message", "Hola! Tienes acceso",
                "userId", principal.getUserId(),
                "tenantId", principal.getTenantId(),
                "email", principal.getEmail(),
                "role", principal.getRole()
        ));
    }
}
