package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.dto.CreateCheckoutRequest;
import com.helpdesk.dto.SubscriptionResponse;
import com.helpdesk.service.BillingService;
import com.helpdesk.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final SubscriptionService subscriptionService;

    public BillingController(BillingService billingService,
                             SubscriptionService subscriptionService) {
        this.billingService = billingService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/me")
    public SubscriptionResponse me(@AuthenticationPrincipal JwtPrincipal principal) {
        return SubscriptionResponse.from(
                subscriptionService.getOrCreateForTenant(principal.getTenantId()));
    }

    @PostMapping("/checkout-session")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> checkout(@AuthenticationPrincipal JwtPrincipal principal,
                                        @Valid @RequestBody CreateCheckoutRequest request) {
        return Map.of("url", billingService.createCheckoutSession(principal.getTenantId(), request));
    }
}
