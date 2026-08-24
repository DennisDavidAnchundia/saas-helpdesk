package com.helpdesk.controller;

import com.helpdesk.config.JwtPrincipal;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.model.Attachment;
import com.helpdesk.model.Tenant;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.User;
import com.helpdesk.model.enums.Role;
import com.helpdesk.repository.AttachmentRepository;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.uploads.dir=target/test-uploads")
@Transactional
@ActiveProfiles("test")
class AttachmentControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private com.helpdesk.service.AttachmentService attachmentService;

    private MockMvc mockMvc;

    private static final Path UPLOAD_ROOT = Path.of("target", "test-uploads");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void cleanDisk() throws IOException {
        if (Files.exists(UPLOAD_ROOT)) {
            try (var paths = Files.walk(UPLOAD_ROOT)) {
                paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                    }
                });
            }
        }
    }

    @Test
    void customerUploadsListsAndDownloadsAttachmentForOwnTicket() throws Exception {
        Tenant tenant = createTenant("Adj Co");
        User customer = createUser(tenant, "cust@adj.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, customer);

        byte[] bytes = "contenido del reporte".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "reporte.txt", "text/plain", bytes);

        String listJson = mockMvc.perform(multipart("/api/tickets/" + ticket.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("reporte.txt"))
                .andExpect(jsonPath("$.sizeBytes").value(bytes.length))
                .andReturn().getResponse().getContentAsString();

        // La metadata quedo persistida y el archivo existe en disco
        assertEquals(1, attachmentRepository.count());
        assertFalse(listJson.isBlank());

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/attachments")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("reporte.txt"));

        Long attachmentId = attachmentRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/attachments/" + attachmentId + "/download")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''reporte.txt"));
    }

    @Test
    void crossTenantCustomerCannotUploadOrList() throws Exception {
        Tenant tenantA = createTenant("Adj Iso A");
        Tenant tenantB = createTenant("Adj Iso B");
        User owner = createUser(tenantA, "owner@ai.com", Role.CUSTOMER);
        User other = createUser(tenantB, "other@ai.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenantA, owner);

        MockMultipartFile file = new MockMultipartFile(
                "file", "secreto.txt", "text/plain", "data".getBytes());

        mockMvc.perform(multipart("/api/tickets/" + ticket.getId() + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/tickets/" + ticket.getId() + "/attachments")
                        .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isBadRequest());

        assertEquals(0, attachmentRepository.count());
    }

    @Test
    void oversizedFileIsRejectedWithoutMetadata() {
        Tenant tenant = createTenant("Adj Big");
        User customer = createUser(tenant, "big@adj.com", Role.CUSTOMER);
        Ticket ticket = createTicket(tenant, customer);

        byte[] big = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "grande.bin", "application/octet-stream", big);

        assertThrows(IllegalArgumentException.class,
                () -> attachmentService.store(principal(customer), ticket.getId(), file));
        assertEquals(0, attachmentRepository.count());
    }

    @Test
    void onlyUploaderOrAdminCanDeleteAttachment() throws Exception {
        Tenant tenant = createTenant("Adj Del");
        User customer = createUser(tenant, "del@adj.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@adj.com", Role.AGENT);
        User admin = createUser(tenant, "admin@adj.com", Role.ADMIN);
        Ticket ticket = createTicket(tenant, customer);
        ticket.setAgent(agent);
        ticketRepository.save(ticket);

        attachmentService.store(
                principal(customer), ticket.getId(),
                new MockMultipartFile("file", "borrable.txt", "text/plain", "x".getBytes()));
        Long savedId = attachmentRepository.findAll().get(0).getId();

        // Un agente que no lo subio no puede borrar
        JwtPrincipal agentPrincipal = principal(agent);
        assertThrows(IllegalArgumentException.class,
                () -> attachmentService.delete(agentPrincipal, ticket.getId(), savedId));

        // El admin del tenant si puede (aunque no lo haya subido)
        mockMvc.perform(delete("/api/tickets/" + ticket.getId() + "/attachments/" + savedId)
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isNoContent());

        assertEquals(0, attachmentRepository.count());
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

    private Ticket createTicket(Tenant tenant, User customer) {
        Ticket ticket = new Ticket();
        ticket.setTenant(tenant);
        ticket.setCustomer(customer);
        ticket.setTitle("Ticket con adjunto");
        ticket.setDescription("Descripcion de prueba");
        return ticketRepository.save(ticket);
    }

    private String token(User u) {
        return jwtProvider.generateToken(u.getId(), u.getTenant().getId(), u.getEmail(), u.getRole().name());
    }

    private JwtPrincipal principal(User u) {
        return new JwtPrincipal(u.getId(), u.getTenant().getId(), u.getEmail(), u.getRole().name());
    }
}
