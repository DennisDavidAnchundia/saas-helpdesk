package com.helpdesk.controller;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserControllerTest {

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void adminListsOnlyActiveAgentsOfOwnTenant() throws Exception {
        Tenant tenant = createTenant("Users Co");
        User admin = createUser(tenant, "admin@users.com", Role.ADMIN);
        createUser(tenant, "agente1@users.com", Role.AGENT);
        createUser(tenant, "agente2@users.com", Role.AGENT);
        User inactiveAgent = createUser(tenant, "inactivo@users.com", Role.AGENT);
        inactiveAgent.setActive(false);
        userRepository.save(inactiveAgent);
        createUser(tenant, "cliente@users.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", hasItems(
                        "agente1@users.com", "agente2@users.com")))
                .andExpect(jsonPath("$[0].fullName").value("agente1"))
                .andExpect(jsonPath("$[0].id").isNumber());
    }

    @Test
    void agentsAreIsolatedPerTenant() throws Exception {
        Tenant tenantA = createTenant("Users Iso A");
        Tenant tenantB = createTenant("Users Iso B");
        User adminA = createUser(tenantA, "admin@a.com", Role.ADMIN);
        createUser(tenantA, "agentea@a.com", Role.AGENT);
        createUser(tenantB, "agenteb@b.com", Role.AGENT);

        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(adminA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("agentea@a.com"));
    }

    @Test
    void agentCanAlsoListAgents() throws Exception {
        Tenant tenant = createTenant("Users Agent Co");
        User agent = createUser(tenant, "yo@agent.com", Role.AGENT);

        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void customerGets403() throws Exception {
        Tenant tenant = createTenant("Users Cust Co");
        User customer = createUser(tenant, "cli@cust.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/users/agents"))
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
