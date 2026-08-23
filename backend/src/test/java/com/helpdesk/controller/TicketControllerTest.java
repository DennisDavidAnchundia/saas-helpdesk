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
class TicketControllerTest {

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
    void customerCreatesTicketReturnsCreatedWithDefaults() throws Exception {
        Tenant tenant = createTenant("Ticket Co");
        User customer = createUser(tenant, "cust@ticketco.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("Mi impresora no anda", "MEDIUM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.slaDueAt", notNullValue()))
                .andExpect(jsonPath("$.agentId").doesNotExist())
                .andExpect(jsonPath("$.customerName").value("cust"));
    }

    @Test
    void ticketsAreIsolatedPerTenant() throws Exception {
        Tenant tenantA = createTenant("Tenant A");
        Tenant tenantB = createTenant("Tenant B");
        User userA = createUser(tenantA, "a@tenant.com", Role.CUSTOMER);
        User userB = createUser(tenantB, "b@tenant.com", Role.CUSTOMER);

        String response = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson("Ticket privado de A", "LOW")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long ticketId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token(userB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/tickets/" + ticketId)
                        .header("Authorization", "Bearer " + token(userB)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidTransitionOpenToClosedIsRejected() throws Exception {
        Tenant tenant = createTenant("Invalid Co");
        User admin = createUser(tenant, "admin@invalid.com", Role.ADMIN);

        MvcResult result = createTicketAs(admin, "Ticket en abierto");
        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=CLOSED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fullHappyFlowSetsTimestampsAtEachStep() throws Exception {
        Tenant tenant = createTenant("Happy Co");
        User admin = createUser(tenant, "admin@happy.com", Role.ADMIN);

        MvcResult result = createTicketAs(admin, "Flujo feliz");
        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.firstResponseAt", notNullValue()));

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=RESOLVED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=CLOSED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closedAt", notNullValue()));
    }

    @Test
    void customerCanOnlyReopenOwnResolvedTicket() throws Exception {
        Tenant tenant = createTenant("Reopen Co");
        User customer = createUser(tenant, "cust@reopen.com", Role.CUSTOMER);
        User admin = createUser(tenant, "admin@reopen.com", Role.ADMIN);

        MvcResult result = createTicketAs(customer, "Problema con factura");
        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=RESOLVED")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=CLOSED")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/status?status=REOPENED")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REOPENED"))
                .andExpect(jsonPath("$.resolvedAt", nullValue()));
    }

    @Test
    void customerCannotAssignAgents() throws Exception {
        Tenant tenant = createTenant("Forbidden Co");
        User customer = createUser(tenant, "cust@forbidden.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@forbidden.com", Role.AGENT);

        MvcResult result = createTicketAs(customer, "No puedo asignar");
        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign/" + agent.getId())
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAssignsAgentManually() throws Exception {
        Tenant tenant = createTenant("Assign Co");
        User admin = createUser(tenant, "admin@assign.com", Role.ADMIN);
        User agent = createUser(tenant, "agent@assign.com", Role.AGENT);

        MvcResult result = createTicketAs(admin, "Necesita agente");
        long ticketId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign/" + agent.getId())
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent.getId()))
                .andExpect(jsonPath("$.agentName").value("agent"));
    }

    @Test
    void agentCannotAssignOtherAgents() throws Exception {
        Tenant tenant = createTenant("Self Only Co");
        User agent1 = createUser(tenant, "agent1@selfonly.com", Role.AGENT);
        User agent2 = createUser(tenant, "agent2@selfonly.com", Role.AGENT);

        long ticketId = createTicketAndGetId(agent1, "Para quien caiga");

        // Un AGENTE no puede reasignar a otro agente
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign/" + agent2.getId())
                        .header("Authorization", "Bearer " + token(agent1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agentCanTakeTicketForItself() throws Exception {
        Tenant tenant = createTenant("Take It Co");
        User agent = createUser(tenant, "agent@takeit.com", Role.AGENT);

        long ticketId = createTicketAndGetId(agent, "Libre");

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign/" + agent.getId())
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent.getId()))
                .andExpect(jsonPath("$.agentName").value("agent"));
    }

    @Test
    void agentAutoAssignTakesTheTicketForItself() throws Exception {
        Tenant tenant = createTenant("Take RR Co");
        User agent1 = createUser(tenant, "agent1@takerr.com", Role.AGENT);
        createUser(tenant, "agent2@takerr.com", Role.AGENT);

        long ticketId = createTicketAndGetId(agent1, "Sin dueno");

        // Aunque agent2 este menos cargado, el auto-asignar de un AGENTE se lo lleva el mismo
        mockMvc.perform(patch("/api/tickets/" + ticketId + "/assign")
                        .header("Authorization", "Bearer " + token(agent1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent1.getId()));
    }

    @Test
    void autoAssignDistributesEvenlyBetweenTwoAgents() throws Exception {
        Tenant tenant = createTenant("Round Robin Co");
        User admin = createUser(tenant, "admin@rr.com", Role.ADMIN);
        User agent1 = createUser(tenant, "agent1@rr.com", Role.AGENT);
        User agent2 = createUser(tenant, "agent2@rr.com", Role.AGENT);

        long ticket1 = createTicketAndGetId(admin, "RR uno");
        long ticket2 = createTicketAndGetId(admin, "RR dos");

        MvcResult r1 = mockMvc.perform(patch("/api/tickets/" + ticket1 + "/assign")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andReturn();
        Long assigned1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("agentId").asLong();

        MvcResult r2 = mockMvc.perform(patch("/api/tickets/" + ticket2 + "/assign")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andReturn();
        Long assigned2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("agentId").asLong();

        org.junit.jupiter.api.Assertions.assertNotEquals(assigned1, assigned2,
                "El segundo ticket debe ir al otro agente");
    }

    @Test
    void listSupportsFiltersAndPagination() throws Exception {
        Tenant tenant = createTenant("Filter Co");
        User admin = createUser(tenant, "admin@filter.com", Role.ADMIN);

        createTicketAsWithPriority(admin, "Uno mediano", "MEDIUM");
        createTicketAsWithPriority(admin, "Dos alto", "HIGH");
        createTicketAsWithPriority(admin, "Tres alto", "HIGH");

        // Filtro por prioridad
        mockMvc.perform(get("/api/tickets?priority=HIGH")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].priority").value("HIGH"));

        // Paginacion: 3 tickets, pagina de 2
        mockMvc.perform(get("/api/tickets?size=2&page=0")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Ultima pagina con el ticket restante
        mockMvc.perform(get("/api/tickets?size=2&page=1")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void invalidStatusParamIsRejected() throws Exception {
        Tenant tenant = createTenant("Bad Param Co");
        User admin = createUser(tenant, "admin@badparam.com", Role.ADMIN);

        mockMvc.perform(get("/api/tickets?status=NOT_A_STATUS")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerSeesOnlyOwnTicketsInList() throws Exception {
        Tenant tenant = createTenant("Portal Co");
        User customer1 = createUser(tenant, "cli1@portal.com", Role.CUSTOMER);
        User customer2 = createUser(tenant, "cli2@portal.com", Role.CUSTOMER);

        createTicketAs(customer1, "Problema propio uno");
        createTicketAs(customer1, "Problema propio dos");
        createTicketAs(customer2, "Ticket de otro cliente");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // El admin sigue viendo todo el tenant
        User admin = createUser(tenant, "admin@portal.com", Role.ADMIN);
        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void customerCannotFetchAnotherCustomersTicketById() throws Exception {
        Tenant tenant = createTenant("Peek Co");
        User customer1 = createUser(tenant, "cli1@peek.com", Role.CUSTOMER);
        User customer2 = createUser(tenant, "cli2@peek.com", Role.CUSTOMER);

        long ownId = createTicketAndGetId(customer1, "Mio y solo mio");
        long otherId = createTicketAndGetId(customer2, "De otro cliente");

        mockMvc.perform(get("/api/tickets/" + otherId)
                        .header("Authorization", "Bearer " + token(customer1)))
                .andExpect(status().isBadRequest());

        // Su propio ticket si es visible
        mockMvc.perform(get("/api/tickets/" + ownId)
                        .header("Authorization", "Bearer " + token(customer1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Mio y solo mio"));
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

    private String ticketJson(String title, String priority) {
        return "{\"title\":\"" + title + "\",\"description\":\"Descripcion de prueba\",\"priority\":\"" + priority + "\"}";
    }

    private MvcResult createTicketAs(User user, String title) throws Exception {
        return createTicketAsWithPriority(user, title, "MEDIUM");
    }

    private MvcResult createTicketAsWithPriority(User user, String title, String priority) throws Exception {
        return mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(title, priority)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long createTicketAndGetId(User user, String title) throws Exception {
        return objectMapper.readTree(createTicketAs(user, title).getResponse().getContentAsString()).get("id").asLong();
    }
}
