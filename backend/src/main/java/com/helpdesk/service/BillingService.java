package com.helpdesk.service;

import com.helpdesk.dto.CreateCheckoutRequest;
import com.helpdesk.model.enums.SubscriptionPlan;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingService {

    private final SubscriptionService subscriptionService;
    private final String pricePro;
    private final String priceEnterprise;
    private final String successUrl;
    private final String cancelUrl;

    public BillingService(SubscriptionService subscriptionService,
                          @Value("${stripe.secret-key}") String secretKey,
                          @Value("${stripe.price-pro}") String pricePro,
                          @Value("${stripe.price-enterprise}") String priceEnterprise,
                          @Value("${stripe.success-url}") String successUrl,
                          @Value("${stripe.cancel-url}") String cancelUrl) {
        this.subscriptionService = subscriptionService;
        this.pricePro = pricePro;
        this.priceEnterprise = priceEnterprise;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        Stripe.apiKey = secretKey;
    }

    public String createCheckoutSession(Long tenantId, CreateCheckoutRequest request) {
        SubscriptionPlan targetPlan = SubscriptionPlan.valueOf(request.getTargetPlan());
        String priceId = targetPlan == SubscriptionPlan.PRO ? pricePro : priceEnterprise;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(String.valueOf(tenantId))
                .putMetadata("tenantId", String.valueOf(tenantId))
                .putMetadata("plan", targetPlan.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPrice(priceId)
                        .build())
                .build();

        try {
            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new IllegalStateException("Error al crear la sesion de pago en Stripe: " + e.getMessage());
        }
    }
}
