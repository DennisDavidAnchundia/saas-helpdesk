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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class DashboardControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void summaryReturnsCountsByStatusTotalsAndTopAgents() throws Exception {
        Tenant tenant = createTenant("Dash Co");
        User admin = createUser(tenant, "admin@dash.com", Role.ADMIN);
        User agent = createUser(tenant, "agent@dash.com", Role.AGENT);

        long resolvedId = createTicketAndGetId(admin, "Se resuelve");
        long assignedId = createTicketAndGetId(admin, "Queda en progreso");
        createTicketAndGetId(admin, "Sigue abierto");

        mockMvc.perform(patch("/api/tickets/" + assignedId + "/assign/" + agent.getId())
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/" + assignedId + "/status?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/" + resolvedId + "/status?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/tickets/" + resolvedId + "/status?status=RESOLVED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(3))
                .andExpect(jsonPath("$.ticketsByStatus.OPEN").value(1))
                .andExpect(jsonPath("$.ticketsByStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.ticketsByStatus.RESOLVED").value(1))
                .andExpect(jsonPath("$.ticketsByStatus.CLOSED").value(0))
                .andExpect(jsonPath("$.topAgents", hasSize(1)))
                .andExpect(jsonPath("$.topAgents[0].agentName").value("agent"))
                .andExpect(jsonPath("$.topAgents[0].assignedTickets").value(1));
    }

    @Test
    void averagesAreComputedAfterResolutionFlow() throws Exception {
        Tenant tenant = createTenant("Avg Co");
        User admin = createUser(tenant, "admin@avg.com", Role.ADMIN);

        long ticketId = createTicketAndGetId(admin, "Con metricas");
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=RESOLVED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgFirstResponseSeconds", notNullValue()))
                .andExpect(jsonPath("$.avgResolutionSeconds", notNullValue()));
    }

    @Test
    void averagesAreNullWhenNothingWasResolved() throws Exception {
        Tenant tenant = createTenant("Null Avg Co");
        User admin = createUser(tenant, "admin@nullavg.com", Role.ADMIN);

        createTicketAndGetId(admin, "Nunca resuelto");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgFirstResponseSeconds").doesNotExist())
                .andExpect(jsonPath("$.avgResolutionSeconds").doesNotExist());
    }

    @Test
    void slaBreachedCountsOnlyOverdueActiveTickets() throws Exception {
        Tenant tenant = createTenant("Sla Co");
        User admin = createUser(tenant, "admin@sla.com", Role.ADMIN);

        long overdueId = createTicketAndGetId(admin, "Vencido");
        long healthyId = createTicketAndGetId(admin, "Al dia");

        jdbcTemplate.update(
                "UPDATE tickets SET sla_due_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(2), overdueId);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaBreachedCount").value(1));

        jdbcTemplate.update(
                "UPDATE tickets SET sla_due_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(2), healthyId);
        jdbcTemplate.update(
                "UPDATE tickets SET status = 'RESOLVED', resolved_at = ? WHERE id = ?",
                LocalDateTime.now(), healthyId);

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slaBreachedCount").value(1));
    }

    @Test
    void summaryIsIsolatedPerTenant() throws Exception {
        Tenant tenantA = createTenant("Iso A");
        Tenant tenantB = createTenant("Iso B");
        User adminA = createUser(tenantA, "admin@a.com", Role.ADMIN);
        User adminB = createUser(tenantB, "admin@b.com", Role.ADMIN);

        createTicketAndGetId(adminA, "De A uno");
        createTicketAndGetId(adminA, "De A dos");

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(adminB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(0))
                .andExpect(jsonPath("$.topAgents", empty()));

        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + token(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(2));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
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

    private String ticketJson(String title) {
        return "{\"title\":\"" + title + "\",\"description\":\"Descripcion de prueba\",\"priority\":\"MEDIUM\"}";
    }

    private long createTicketAndGetId(User user, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
