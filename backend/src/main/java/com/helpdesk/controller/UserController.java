package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.AgentResponse;
import com.helpdesk.dto.CreateUserRequest;
import com.helpdesk.dto.ResetPasswordRequest;
import com.helpdesk.dto.UpdateUserActiveRequest;
import com.helpdesk.dto.UserResponse;
import com.helpdesk.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Agentes activos del tenant (para selects de asignacion). */
    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public List<AgentResponse> agents(@AuthenticationPrincipal JwtPrincipal principal) {
        return userService.listActiveAgents(principal.getTenantId());
    }

    /** Todos los usuarios del tenant (panel admin). */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> users(@AuthenticationPrincipal JwtPrincipal principal) {
        return userService.listUsers(principal.getTenantId());
    }

    /** Crear un agente del tenant (panel admin). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request,
                               @AuthenticationPrincipal JwtPrincipal principal) {
        return userService.createAgent(principal.getTenantId(), request);
    }

    /** Activar / desactivar un agente del tenant (panel admin). */
    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse setActive(@PathVariable Long id,
                                  @Valid @RequestBody UpdateUserActiveRequest request,
                                  @AuthenticationPrincipal JwtPrincipal principal) {
        return userService.setUserActive(
                principal.getTenantId(), id, Boolean.TRUE.equals(request.getIsActive()));
    }

    /** Reset de contraseña de un agente (panel admin). */
    @PatchMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @Valid @RequestBody ResetPasswordRequest request,
                                              @AuthenticationPrincipal JwtPrincipal principal) {
        userService.resetAgentPassword(principal.getTenantId(), id, request.getNewPassword());
        return ResponseEntity.noContent().build();
    }
}
