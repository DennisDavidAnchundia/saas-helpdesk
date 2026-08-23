package com.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.model.Subscription;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.model.enums.SubscriptionPlan;
import com.helpdesk.model.enums.SubscriptionStatus;
import com.helpdesk.repository.SubscriptionRepository;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class StripeWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_123";

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void checkoutSessionCompletedActivatesProPlan() throws Exception {
        Tenant tenant = createTenant("Webhook Co");
        User admin = createUser(tenant, "admin@webhook.com", Role.ADMIN);

        Map<String, Object> session = Map.of(
                "object", "checkout.session",
                "customer", "cus_test_123",
                "subscription", "sub_test_456",
                "metadata", Map.of(
                        "tenantId", String.valueOf(tenant.getId()),
                        "plan", "PRO"));
        String payload = eventJson("checkout.session.completed", session);

        mockMvc.perform(post("/api/stripe/webhook")
                        .header("Stripe-Signature", sign(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        Subscription subscription = subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow();
        assertThat(subscription.getPlan()).isEqualTo(SubscriptionPlan.PRO);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStripeCustomerId()).isEqualTo("cus_test_123");
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_test_456");
        assertThat(subscription.getTicketsLimit()).isEqualTo(5000);
        assertThat(subscription.getTicketsUsed()).isZero();
        assertThat(admin).isNotNull();
    }

    @Test
    void paymentFailedMarksPastDue() throws Exception {
        Tenant tenant = createTenant("PastDue Co");
        Subscription subscription = subscriptionRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createFree(tenant));
        subscription.setStripeCustomerId("cus_pastdue_1");
        subscription.setPlan(SubscriptionPlan.PRO);
        subscriptionRepository.save(subscription);

        Map<String, Object> invoice = Map.of(
                "object", "invoice",
                "customer", "cus_pastdue_1");
        String payload = eventJson("invoice.payment_failed", invoice);

        mockMvc.perform(post("/api/stripe/webhook")
                        .header("Stripe-Signature", sign(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow()
                .getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
    }

    @Test
    void invalidSignatureIsRejected() throws Exception {
        Tenant tenant = createTenant("Bad Sig Co");
        String payload = eventJson("checkout.session.completed", Map.of("object", "checkout.session"));

        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("otra-clave-distinta".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        String badSignature = "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hash);

        mockMvc.perform(post("/api/stripe/webhook")
                        .header("Stripe-Signature", badSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        assertThat(subscriptionRepository.findByTenantId(tenant.getId())).isEmpty();
    }

    @Test
    void missingSignatureHeaderIsRejected() throws Exception {
        createTenant("No Header Co");
        String payload = eventJson("checkout.session.completed", Map.of("object", "checkout.session"));

        mockMvc.perform(post("/api/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownEventTypeIsAcceptedWithoutChanges() throws Exception {
        Tenant tenant = createTenant("Unknown Co");
        createFree(tenant);

        String payload = eventJson("charge.refunded", Map.of("object", "charge", "amount", 1000));

        mockMvc.perform(post("/api/stripe/webhook")
                        .header("Stripe-Signature", sign(payload))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        assertThat(subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow()
                .getPlan()).isEqualTo(SubscriptionPlan.FREE);
    }

    // ---------- helpers ----------

    private String eventJson(String type, Map<String, Object> dataObject) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "id", "evt_" + System.nanoTime(),
                "object", "event",
                "type", type,
                "data", Map.of("object", dataObject)));
    }

    private String sign(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(hash);
    }

    private Tenant createTenant(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]", "") + System.nanoTime();
        return tenantRepository.save(new Tenant(name, slug));
    }

    private User createUser(Tenant tenant, String email, Role role) {
        return userRepository.save(new User(
                tenant, email, passwordEncoder.encode("password123"),
                email.split("@")[0], role));
    }

    private Subscription createFree(Tenant tenant) {
        Subscription subscription = new Subscription();
        subscription.setTenant(tenant);
        subscriptionRepository.save(subscription);
        return subscription;
    }
}
