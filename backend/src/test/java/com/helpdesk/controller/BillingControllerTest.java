package com.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.model.Subscription;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.model.enums.SubscriptionPlan;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class BillingControllerTest {

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
    void meCreatesFreeSubscriptionWithDefaultsOnFirstCall() throws Exception {
        Tenant tenant = createTenant("Billing Me Co");
        User admin = createUser(tenant, "admin@bme.com", Role.ADMIN);

        mockMvc.perform(get("/api/billing/me")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.ticketsUsed").value(0))
                .andExpect(jsonPath("$.ticketsLimit").value(100))
                .andExpect(jsonPath("$.currentPeriodEnd", org.hamcrest.Matchers.notNullValue()));

        assertThat(subscriptionRepository.findByTenantId(tenant.getId())).isPresent();
    }

    @Test
    void checkoutRequiresAdminRole() throws Exception {
        Tenant tenant = createTenant("Checkout Role Co");
        User customer = createUser(tenant, "cust@crole.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/billing/checkout-session")
                        .header("Authorization", "Bearer " + token(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPlan\":\"PRO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkoutRejectsInvalidPlanBeforeCallingStripe() throws Exception {
        Tenant tenant = createTenant("Checkout Plan Co");
        User admin = createUser(tenant, "admin@cplan.com", Role.ADMIN);

        mockMvc.perform(post("/api/billing/checkout-session")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetPlan\":\"ULTRA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ticketCreationIsBlockedWhenQuotaExhausted() throws Exception {
        Tenant tenant = createTenant("Quota Co");
        User admin = createUser(tenant, "admin@quota.com", Role.ADMIN);
        createFreeWith(tenant, sub -> {
            sub.setTicketsUsed(100);
            sub.setTicketsLimit(100);
            sub.setPlan(SubscriptionPlan.FREE);
        });

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No debe crearse\",\"description\":\"x\",\"priority\":\"LOW\"}"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.message").value(
                        "Alcanzaste el limite de 100 tickets/mes del plan FREE. Mejora tu plan para crear mas tickets."));
    }

    @Test
    void ticketCreationUnderLimitIncrementsUsage() throws Exception {
        Tenant tenant = createTenant("Usage Co");
        User admin = createUser(tenant, "admin@usage.com", Role.ADMIN);
        createFreeWith(tenant, sub -> {
            sub.setTicketsUsed(2);
            sub.setTicketsLimit(5);
        });

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dentro del limite\",\"description\":\"x\",\"priority\":\"LOW\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(ticketId).isPositive();

        Subscription subscription = subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow();
        assertThat(subscription.getTicketsUsed()).isEqualTo(3);
    }

    // ---------- helpers ----------

    private Tenant createTenant(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]", "") + System.nanoTime();
        return tenantRepository.save(new Tenant(name, slug));
    }

    private User createUser(Tenant tenant, String email, Role role) {
        return userRepository.save(new User(
                tenant, email, passwordEncoder.encode("password123"),
                email.split("@")[0], role));
    }

    private String token(User u) {
        return jwtProvider.generateToken(u.getId(), u.getTenant().getId(), u.getEmail(), u.getRole().name());
    }

    private Subscription createFreeWith(Tenant tenant, java.util.function.Consumer<Subscription> customizer) {
        Subscription subscription = new Subscription();
        subscription.setTenant(tenant);
        customizer.accept(subscription);
        return subscriptionRepository.save(subscription);
    }
}
