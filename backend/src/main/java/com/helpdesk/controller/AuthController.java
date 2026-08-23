package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.ChangePasswordRequest;
import com.helpdesk.dto.JoinRequest;
import com.helpdesk.dto.LoginRequest;
import com.helpdesk.dto.LoginResponse;
import com.helpdesk.dto.RegisterRequest;
import com.helpdesk.dto.RegisterResponse;
import com.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Registro publico como cliente de una empresa existente. */
    @PostMapping("/join")
    public ResponseEntity<RegisterResponse> join(@Valid @RequestBody JoinRequest request) {
        RegisterResponse response = authService.join(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /** Cambio de contrasena propia (requiere la actual). */
    @PatchMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request,
                                                  @AuthenticationPrincipal JwtPrincipal principal) {
        authService.changeOwnPassword(principal.getUserId(), request);
        return ResponseEntity.noContent().build();
    }
}
