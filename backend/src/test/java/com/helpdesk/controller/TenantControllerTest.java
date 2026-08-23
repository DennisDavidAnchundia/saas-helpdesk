package com.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
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

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TenantControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

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
    void defaultSlaPolicyMatchesStandardDefaults() throws Exception {
        Tenant tenant = createTenant("Sla Def Co");
        User admin = createUser(tenant, "admin@sladef.com", Role.ADMIN);

        mockMvc.perform(get("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgentHours").value(4))
                .andExpect(jsonPath("$.highHours").value(8))
                .andExpect(jsonPath("$.mediumHours").value(24))
                .andExpect(jsonPath("$.lowHours").value(72));
    }

    @Test
    void updatedSlaPolicyAppliesToNewTickets() throws Exception {
        Tenant tenant = createTenant("Sla Upd Co");
        User admin = createUser(tenant, "admin@slaupd.com", Role.ADMIN);

        mockMvc.perform(put("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urgentHours\":2,\"highHours\":16}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urgentHours").value(2))
                .andExpect(jsonPath("$.highHours").value(16))
                .andExpect(jsonPath("$.mediumHours").value(24))
                .andExpect(jsonPath("$.lowHours").value(72));

        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Urgente con SLA propio\",\"description\":\"Descripcion de prueba\",\"priority\":\"URGENT\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String slaDueAt = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("slaDueAt").asText();
        long hours = Duration.between(
                LocalDateTime.now(),
                LocalDateTime.parse(slaDueAt)).toMinutes();
        // ~2 horas de margen: 110 a 130 minutos
        assertTrue(hours >= 110 && hours <= 130,
                "slaDueAt deberia estar a unas 2 horas, fue de " + hours + " minutos");
    }

    @Test
    void slaValidationRejectsOutOfRangeValues() throws Exception {
        Tenant tenant = createTenant("Sla Bad Co");
        User admin = createUser(tenant, "admin@slabad.com", Role.ADMIN);

        mockMvc.perform(put("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urgentHours\":0}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lowHours\":10000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdminCanAccessSlaEndpoints() throws Exception {
        Tenant tenant = createTenant("Sla Role Co");
        User agent = createUser(tenant, "agente@slarole.com", Role.AGENT);
        User customer = createUser(tenant, "cliente@slarole.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/tenants/sla")
                        .header("Authorization", "Bearer " + token(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"urgentHours\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/tenants/sla"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private Tenant createTenant(String name) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]", "");
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
}
