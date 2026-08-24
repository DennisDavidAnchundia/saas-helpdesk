package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.dto.ChatMessageResponse;
import com.helpdesk.dto.SendChatMessageRequest;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.MessageRepository;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.TicketRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private com.helpdesk.service.ChatService chatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void historyReturnsMessagesInOrderForTicketOwner() throws Exception {
        Tenant tenant = createTenant("Chat Co");
        User customer = createUser(tenant, "cust@chat.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@chat.com", Role.AGENT);
        Ticket ticket = createTicket(tenant, customer, agent);

        send(customer, ticket.getId(), "Hola, mi impresora no anda");
        send(agent, ticket.getId(), "Ya la reviso");

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].content").value("Hola, mi impresora no anda"))
                .andExpect(jsonPath("$.content[0].senderName").value("cust"))
                .andExpect(jsonPath("$.content[1].content").value("Ya la reviso"));
    }

    @Test
    void historyIsPagedStartingFromNewestMessages() throws Exception {
        Tenant tenant = createTenant("Pager Chat Co");
        User customer = createUser(tenant, "cust@pgc.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, customer, null);

        for (int i = 1; i <= 5; i++) {
            send(customer, ticket.getId(), "Mensaje " + i);
        }

        // Pagina 0 con size 2: los dos mas recientes (4 y 5), en orden cronologico
        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .param("page", "0")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].content").value("Mensaje 4"))
                .andExpect(jsonPath("$.content[1].content").value("Mensaje 5"));

        // La ultima pagina trae solo el mensaje mas viejo
        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .param("page", "2")
                        .param("size", "2")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Mensaje 1"));
    }

    @Test
    void customerCannotReadOtherUsersTicketHistory() throws Exception {
        Tenant tenant = createTenant("Private Chat Co");
        User owner = createUser(tenant, "owner@pc.com", Role.CUSTOMER);
        User other = createUser(tenant, "other@pc.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, owner, null);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assignedAgentCanReadHistoryButUnassignedCannot() throws Exception {
        Tenant tenant = createTenant("Agents Chat Co");
        User customer = createUser(tenant, "cust@ac.com", Role.CUSTOMER);
        User assigned = createUser(tenant, "a1@ac.com", Role.AGENT);
        User unassigned = createUser(tenant, "a2@ac.com", Role.AGENT);
        Ticket ticket = createTicket(tenant, customer, assigned);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(assigned)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(unassigned)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminOfTenantCanReadAnyTicketHistory() throws Exception {
        Tenant tenant = createTenant("Admin Chat Co");
        User customer = createUser(tenant, "cust@adc.com", Role.CUSTOMER);
        User admin = createUser(tenant, "admin@adc.com", Role.ADMIN);
        Ticket ticket = createTicket(tenant, customer, null);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void crossTenantHistoryIsRejected() throws Exception {
        Tenant tenantA = createTenant("Chat Iso A");
        Tenant tenantB = createTenant("Chat Iso B");
        User custA = createUser(tenantA, "a@ci.com", Role.CUSTOMER);
        User custB = createUser(tenantB, "b@ci.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenantA, custA, null);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages")
                        .header("Authorization", "Bearer " + token(custB)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotAccessHistoryOrPresence() throws Exception {
        Tenant tenant = createTenant("No Auth Chat");
        User customer = createUser(tenant, "cust@na.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, customer, null);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/messages"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/presence"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void presenceReturnsOnlineListForAccessibleTicket() throws Exception {
        Tenant tenant = createTenant("Presence Co");
        User customer = createUser(tenant, "cust@pres.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@pres.com", Role.AGENT);
        Ticket ticket = createTicket(tenant, customer, agent);

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/presence")
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isOk());
    }

    @Test
    void sendMessageRejectsNonParticipantAndPersistsForOwner() {
        Tenant tenant = createTenant("Persist Co");
        User owner = createUser(tenant, "owner@per.com", Role.CUSTOMER);
        User stranger = createUser(tenant, "stranger@per.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, owner, null);

        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setContent("Mensaje del dueno");

        JwtPrincipal strangerPrincipal =
                principal(stranger);
        assertThrows(IllegalArgumentException.class,
                () -> chatService.send(strangerPrincipal, ticket.getId(), request));
        assertEquals(0, messageRepository.count());

        ChatMessageResponse response = chatService.send(principal(owner), ticket.getId(), request);

        assertEquals(owner.getId(), response.getSenderId());
        assertEquals(ticket.getId(), response.getTicketId());
        assertEquals(1, messageRepository.count());
        assertEquals(request.getContent(),
                messageRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()).get(0).getContent());
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

    private Ticket createTicket(Tenant tenant, User customer, User agent) {
        Ticket ticket = new Ticket();
        ticket.setTenant(tenant);
        ticket.setCustomer(customer);
        if (agent != null) {
            ticket.setAgent(agent);
        }
        ticket.setTitle("Ticket de chat");
        ticket.setDescription("Descripcion de prueba");
        return ticketRepository.save(ticket);
    }

    private String token(User u) {
        return jwtProvider.generateToken(u.getId(), u.getTenant().getId(), u.getEmail(), u.getRole().name());
    }

    private JwtPrincipal principal(User u) {
        return new JwtPrincipal(u.getId(), u.getTenant().getId(), u.getEmail(), u.getRole().name());
    }

    private void send(User sender, Long ticketId, String content) {
        SendChatMessageRequest request = new SendChatMessageRequest();
        request.setContent(content);
        chatService.send(principal(sender), ticketId, request);
    }
}
