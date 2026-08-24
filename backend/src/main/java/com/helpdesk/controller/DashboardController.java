package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.DashboardSummaryResponse;
import com.helpdesk.dto.TrendPointResponse;
import com.helpdesk.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal JwtPrincipal principal) {
        return dashboardService.summary(principal.getTenantId());
    }

    @GetMapping("/trend")
    public List<TrendPointResponse> trend(@AuthenticationPrincipal JwtPrincipal principal) {
        return dashboardService.trend(principal.getTenantId());
    }
}
