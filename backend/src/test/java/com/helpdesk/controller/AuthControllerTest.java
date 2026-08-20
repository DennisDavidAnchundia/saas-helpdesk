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
}
