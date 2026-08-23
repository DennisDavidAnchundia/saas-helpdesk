package com.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.dto.LoginRequest;
import com.helpdesk.dto.RegisterRequest;
import com.helpdesk.repository.TenantRepository;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.AuthService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        com.helpdesk.config.JwtProvider jwtProvider = new com.helpdesk.config.JwtProvider(
                "testSecretKeyForTestingOnly12345678901234567890", 86400000);
        AuthService authService = new AuthService(userRepository, tenantRepository, passwordEncoder, jwtProvider);
        AuthController authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new com.helpdesk.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerWithValidDataReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Dennis Anchundia",
                "dennis@test.com",
                "password123",
                "Mi Empresa"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("dennis@test.com"))
                .andExpect(jsonPath("$.fullName").value("Dennis Anchundia"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tenantName").value("Mi Empresa"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.tenantId").isNumber());
    }

    @Test
    void registerWithInvalidEmailReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Dennis Anchundia",
                "invalid-email",
                "password123",
                "Mi Empresa"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithShortPasswordReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Dennis Anchundia",
                "dennis@test.com",
                "123",
                "Mi Empresa"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithMissingFieldsReturnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithValidCredentialsReturnsToken() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "Dennis Anchundia",
                "dennis@test.com",
                "password123",
                "Mi Empresa"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login
        LoginRequest loginRequest = new LoginRequest(
                "dennis@test.com",
                "password123",
                "mi-empresa"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("dennis@test.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.tenantName").value("Mi Empresa"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
                "Dennis Anchundia",
                "dennis@test.com",
                "password123",
                "Mi Empresa"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Then login with wrong password
        LoginRequest loginRequest = new LoginRequest(
                "dennis@test.com",
                "wrongpassword",
                "mi-empresa"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithNonExistentUserReturnsBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                "nonexistent@test.com",
                "password123",
            "mi-empresa"
        );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    // ---------- join (registro de clientes en empresa existente) ----------

    private void seedTenantWithAdmin(String name, String slug, String adminEmail) {
        com.helpdesk.model.Tenant tenant = tenantRepository.save(new com.helpdesk.model.Tenant(name, slug));
        userRepository.save(new com.helpdesk.model.User(
                tenant, adminEmail, passwordEncoder.encode("password123"), "Dennis", com.helpdesk.model.enums.Role.ADMIN));
    }

    @Test
    void joinCreatesCustomerInExistingTenant() throws Exception {
        seedTenantWithAdmin("Empresa Join", "empresa-join", "admin@join.com");

        com.helpdesk.dto.JoinRequest request = new com.helpdesk.dto.JoinRequest(
                "Carla Cliente", "carla@join.com", "password123", "empresa-join");

        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.email").value("carla@join.com"))
                .andExpect(jsonPath("$.tenantSlug").value("empresa-join"))
                .andExpect(jsonPath("$.userId").isNumber());

        // El cliente recien creado puede loguear contra esa empresa
        LoginRequest loginRequest = new LoginRequest("carla@join.com", "password123", "empresa-join");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void joinWithUnknownTenantSlugIsRejected() throws Exception {
        com.helpdesk.dto.JoinRequest request = new com.helpdesk.dto.JoinRequest(
                "Alguien", "alguien@nowhere.com", "password123", "no-existe");

        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinWithDuplicateEmailInTenantIsRejected() throws Exception {
        seedTenantWithAdmin("Empresa Dup Join", "dup-join", "admin@dupjoin.com");

        com.helpdesk.dto.JoinRequest request = new com.helpdesk.dto.JoinRequest(
                "Impostor", "admin@dupjoin.com", "password123", "dup-join");

        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void joinValidatesPayload() throws Exception {
        seedTenantWithAdmin("Validate Join Co", "validate-join", "admin@vj.com");

        // Contraseña corta
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Corto\",\"email\":\"corto@vj.com\",\"password\":\"123\",\"tenantSlug\":\"validate-join\"}"))
                .andExpect(status().isBadRequest());

        // Email invalido
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Malo\",\"email\":\"no-email\",\"password\":\"password123\",\"tenantSlug\":\"validate-join\"}"))
                .andExpect(status().isBadRequest());

        // Slug vacio
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Vacio\",\"email\":\"vacio@vj.com\",\"password\":\"password123\",\"tenantSlug\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
