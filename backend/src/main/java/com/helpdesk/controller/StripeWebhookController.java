package com.helpdesk.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.model.enums.SubscriptionPlan;
import com.helpdesk.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String webhookSecret;

    public StripeWebhookController(SubscriptionService subscriptionService,
                                   @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.subscriptionService = subscriptionService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handle(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {

        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Webhook no configurado"));
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Falta la cabecera Stripe-Signature"));
        }

        try {
            Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Firma invalida"));
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("type").asText();
            JsonNode object = root.path("data").path("object");

            switch (type) {
                case "checkout.session.completed" -> handleCheckoutCompleted(object);
                case "invoice.payment_failed" -> handlePaymentFailed(object);
                default -> {
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo procesar el evento: " + e.getMessage()));
        }

        return ResponseEntity.ok(Map.of("received", true));
    }

    private void handleCheckoutCompleted(JsonNode session) {
        Long tenantId = Long.parseLong(session.path("metadata").path("tenantId").asText());
        SubscriptionPlan plan = SubscriptionPlan.valueOf(session.path("metadata").path("plan").asText());
        String customerId = session.path("customer").asText();
        String subscriptionId = session.path("subscription").asText();
        subscriptionService.activate(tenantId, customerId, subscriptionId, plan);
    }

    private void handlePaymentFailed(JsonNode invoice) {
        String customerId = invoice.path("customer").asText();
        if (!customerId.isBlank() && !"null".equals(customerId)) {
            subscriptionService.markPastDue(customerId);
        }
    }
}
