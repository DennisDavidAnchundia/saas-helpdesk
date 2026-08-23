package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.AgentResponse;
import com.helpdesk.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/agents")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public List<AgentResponse> agents(@AuthenticationPrincipal JwtPrincipal principal) {
        return userService.listActiveAgents(principal.getTenantId());
    }
}
