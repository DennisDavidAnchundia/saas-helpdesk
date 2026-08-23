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
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void adminListsAllUsersOfOwnTenantOnly() throws Exception {
        Tenant tenantA = createTenant("Panel A");
        Tenant tenantB = createTenant("Panel B");
        User admin = createUser(tenantA, "boss@panel.com", Role.ADMIN);
        createUser(tenantA, "agente@panel.com", Role.AGENT);
        createUser(tenantA, "cliente@panel.com", Role.CUSTOMER);
        createUser(tenantB, "otro@panelb.com", Role.AGENT);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].email", hasItems(
                        "boss@panel.com", "agente@panel.com", "cliente@panel.com")))
                .andExpect(jsonPath("$[*].role", hasItems("ADMIN", "AGENT", "CUSTOMER")))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void adminDeactivatesAndReactivatesAgent() throws Exception {
        Tenant tenant = createTenant("Toggle Co");
        User admin = createUser(tenant, "admin@toggle.com", Role.ADMIN);
        User agent = createUser(tenant, "agente@toggle.com", Role.AGENT);

        mockMvc.perform(patch("/api/users/" + agent.getId() + "/active")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // El agente desactivado sale del listado de agentes activos
        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(patch("/api/users/" + agent.getId() + "/active")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void cannotToggleAgentsFromAnotherTenantOrNonAgentUsers() throws Exception {
        Tenant tenantA = createTenant("Cross Toggle A");
        Tenant tenantB = createTenant("Cross Toggle B");
        User adminA = createUser(tenantA, "admin@xtoggle.com", Role.ADMIN);
        User agentB = createUser(tenantB, "agenteb@xtoggle.com", Role.AGENT);
        User customerA = createUser(tenantA, "cliente@xtoggle.com", Role.CUSTOMER);

        // Usuario de otro tenant -> no encontrado para este admin
        mockMvc.perform(patch("/api/users/" + agentB.getId() + "/active")
                        .header("Authorization", "Bearer " + token(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isBadRequest());

        // Customers y admins no se tocan por esta via
        mockMvc.perform(patch("/api/users/" + customerA.getId() + "/active")
                        .header("Authorization", "Bearer " + token(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/users/" + adminA.getId() + "/active")
                        .header("Authorization", "Bearer " + token(adminA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agentAndCustomerCannotListUsers() throws Exception {
        Tenant tenant = createTenant("No List Co");
        User agent = createUser(tenant, "agente@nolist.com", Role.AGENT);
        User customer = createUser(tenant, "cliente@nolist.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesAgent() throws Exception {
        Tenant tenant = createTenant("Create Co");
        User admin = createUser(tenant, "admin@create.com", Role.ADMIN);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Nuevo Agente\",\"email\":\"nuevo@create.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("nuevo@create.com"))
                .andExpect(jsonPath("$.role").value("AGENT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.fullName").value("Nuevo Agente"));

        // El agente creado aparece en el listado de agentes activos
        mockMvc.perform(get("/api/users/agents")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItems("nuevo@create.com")));
    }

    @Test
    void duplicateEmailInTenantIsRejected() throws Exception {
        Tenant tenant = createTenant("Dup Co");
        User admin = createUser(tenant, "admin@dup.com", Role.ADMIN);
        createUser(tenant, "agente@dup.com", Role.AGENT);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Duplicado\",\"email\":\"agente@dup.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createValidatesPayload() throws Exception {
        Tenant tenant = createTenant("Validate Co");
        User admin = createUser(tenant, "admin@validate.com", Role.ADMIN);

        // Contraseña corta
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Corto\",\"email\":\"corto@validate.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());

        // Email invalido
        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Malo\",\"email\":\"no-es-email\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agentAndCustomerCannotCreateUsers() throws Exception {
        Tenant tenant = createTenant("No Create Co");
        User agent = createUser(tenant, "agente@nocreate.com", Role.AGENT);
        User customer = createUser(tenant, "cliente@nocreate.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Agente Falso\",\"email\":\"x@nocreate.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente Falso\",\"email\":\"y@nocreate.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
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
