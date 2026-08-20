package com.helpdesk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helpdesk.config.JwtProvider;
import com.helpdesk.dto.LoginRequest;
import com.helpdesk.dto.RegisterRequest;
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

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AuthSecurityTest {

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
    void validAdminTokenCanAccessAdminEndpoint() throws Exception {
        String token = registerAndLogin("admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void agentTokenCannotAccessAdminEndpoint() throws Exception {
        String token = registerAndLogin("agent@test.com", "AGENT");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerTokenCannotAccessAdminEndpoint() throws Exception {
        String token = registerAndLogin("customer@test.com", "CUSTOMER");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validAgentTokenCanAccessAgentEndpoint() throws Exception {
        String token = registerAndLogin("agent@test.com", "AGENT");

        mockMvc.perform(get("/api/test/agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("agent@test.com"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }

    @Test
    void adminTokenCannotAccessAgentEndpoint() throws Exception {
        String token = registerAndLogin("admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/test/agent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validCustomerTokenCanAccessCustomerEndpoint() throws Exception {
        String token = registerAndLogin("customer@test.com", "CUSTOMER");

        mockMvc.perform(get("/api/test/customer")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void adminTokenCannotAccessCustomerEndpoint() throws Exception {
        String token = registerAndLogin("admin@test.com", "ADMIN");

        mockMvc.perform(get("/api/test/customer")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void anyRoleCanAccessAnyEndpoint() throws Exception {
        String adminToken = registerAndLogin("admin@test.com", "ADMIN");
        String agentToken = registerAndLogin("agent@test.com", "AGENT");
        String customerToken = registerAndLogin("customer@test.com", "CUSTOMER");

        mockMvc.perform(get("/api/test/any").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/test/any").header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/test/any").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/test/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyBearerTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        JwtProvider shortLivedProvider = new JwtProvider(
                "testSecretKeyForTestingOnly12345678901234567890", 1);
        Thread.sleep(50);
        String expiredToken = shortLivedProvider.generateToken(1L, 1L, "test@test.com", "ADMIN");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongSecretTokenReturns401() throws Exception {
        JwtProvider wrongProvider = new JwtProvider(
                "wrongSecretKeyForTestingOnly12345678901234567890", 86400000);
        String wrongToken = wrongProvider.generateToken(1L, 1L, "test@test.com", "ADMIN");

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + wrongToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndAccessProtectedEndpoint() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Test Co", "user@test.com", "password123", "Test Co");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("user@test.com", "password123", "test-co");
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/test/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private String registerAndLogin(String email, String role) {
        String tenantName = email.split("@")[0] + "-company";
        String slug = tenantName.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");

        Tenant tenant = new Tenant(tenantName, slug);
        tenant = tenantRepository.save(tenant);

        User user = new User(
                tenant, email, passwordEncoder.encode("password123"),
                email.split("@")[0], Role.valueOf(role)
        );
        userRepository.save(user);

        return jwtProvider.generateToken(user.getId(), tenant.getId(), email, role);
    }
}
