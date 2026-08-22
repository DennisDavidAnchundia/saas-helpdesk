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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ArticleControllerTest {

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
    void adminCreatesArticleAsDraftByDefault() throws Exception {
        Tenant tenant = createTenant("Kb Co");
        User admin = createUser(tenant, "admin@kb.com", Role.ADMIN);

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Como resetear su contrasena", "Pasos para resetear", null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.isPublished").value(false))
                .andExpect(jsonPath("$.authorName").value("admin"))
                .andExpect(jsonPath("$.category").value(nullValue()));
    }

    @Test
    void customerCannotCreateArticles() throws Exception {
        Tenant tenant = createTenant("No Kb Co");
        User customer = createUser(tenant, "cust@nokb.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Articulo de cliente", "Contenido", null, true)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersSeeOnlyPublishedArticlesInList() throws Exception {
        Tenant tenant = createTenant("Draft Co");
        User customer = createUser(tenant, "cust@draft.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@draft.com", Role.AGENT);

        createArticle(agent, "Guia publicada", "Contenido publico", "Guias", true);
        createArticle(agent, "Borrador secreto", "Contenido interno", null, false);

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Guia publicada"))
                .andExpect(jsonPath("$[0].isPublished").value(true));
    }

    @Test
    void staffSeesAllArticlesIncludingDrafts() throws Exception {
        Tenant tenant = createTenant("Staff Co");
        User agent = createUser(tenant, "agent@staff.com", Role.AGENT);

        createArticle(agent, "Publicada uno", "Texto uno", null, true);
        createArticle(agent, "Borrador dos", "Texto dos", null, false);

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void searchReturnsMatchingPublishedArticlesForCustomer() throws Exception {
        Tenant tenant = createTenant("Search Co");
        User customer = createUser(tenant, "cust@search.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@search.com", Role.AGENT);

        createArticle(agent, "Como resetear su contrasena", "Use el boton olvidar contrasena", "Cuentas", true);
        createArticle(agent, "Configurar impresora", "Instale los drivers", "Hardware", true);
        createArticle(agent, "Resetear MFA sin acceso", "Borrador interno de MFA", null, false);

        mockMvc.perform(get("/api/articles?q=resetear")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Como resetear su contrasena"));

        mockMvc.perform(get("/api/articles?q=impresora")
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Configurar impresora"));
    }

    @Test
    void staffSearchIncludesUnpublishedArticles() throws Exception {
        Tenant tenant = createTenant("Staff Search Co");
        User admin = createUser(tenant, "admin@staffsearch.com", Role.ADMIN);

        createArticle(admin, "Resetear MFA sin acceso", "Borrador interno de MFA", null, false);

        mockMvc.perform(get("/api/articles?q=mfa")
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isPublished").value(false));
    }

    @Test
    void customerCannotReadUnpublishedArticleById() throws Exception {
        Tenant tenant = createTenant("Hidden Co");
        User customer = createUser(tenant, "cust@hidden.com", Role.CUSTOMER);
        User agent = createUser(tenant, "agent@hidden.com", Role.AGENT);

        long draftId = createArticleAndGetId(agent, "Politica interna", "No visible para clientes", null, false);

        mockMvc.perform(get("/api/articles/" + draftId)
                        .header("Authorization", "Bearer " + token(customer)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/articles/" + draftId)
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Politica interna"));
    }

    @Test
    void updateChangesFieldsAndCanPublish() throws Exception {
        Tenant tenant = createTenant("Update Co");
        User agent = createUser(tenant, "agent@update.com", Role.AGENT);

        long articleId = createArticleAndGetId(agent, "Titulo viejo", "Contenido viejo", "Vieja", false);

        mockMvc.perform(put("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + token(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("Titulo nuevo", "Contenido nuevo", "Nueva", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titulo nuevo"))
                .andExpect(jsonPath("$.content").value("Contenido nuevo"))
                .andExpect(jsonPath("$.category").value("Nueva"))
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    @Test
    void deleteIsAdminOnly() throws Exception {
        Tenant tenant = createTenant("Delete Co");
        User admin = createUser(tenant, "admin@delete.com", Role.ADMIN);
        User agent = createUser(tenant, "agent@delete.com", Role.AGENT);

        long articleId = createArticleAndGetId(admin, "Para borrar", "Contenido a borrar", null, true);

        mockMvc.perform(delete("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + token(agent)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void articlesAreIsolatedPerTenant() throws Exception {
        Tenant tenantA = createTenant("Isolation A");
        Tenant tenantB = createTenant("Isolation B");
        User agentA = createUser(tenantA, "agent@a-iso.com", Role.AGENT);
        User custB = createUser(tenantB, "cust@b-iso.com", Role.CUSTOMER);

        long articleId = createArticleAndGetId(agentA, "Articulo privado de A", "Solo tenant A", null, true);

        mockMvc.perform(get("/api/articles")
                        .header("Authorization", "Bearer " + token(custB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + token(custB)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/articles?q=privado")
                        .header("Authorization", "Bearer " + token(custB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
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

    private String articleJson(String title, String content, String category, Boolean isPublished) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"title\":\"").append(title).append("\",");
        sb.append("\"content\":\"").append(content).append("\"");
        if (category != null) {
            sb.append(",\"category\":\"").append(category).append("\"");
        }
        if (isPublished != null) {
            sb.append(",\"isPublished\":").append(isPublished);
        }
        sb.append("}");
        return sb.toString();
    }

    private MvcResult createArticle(User author, String title, String content,
                                    String category, Boolean isPublished) throws Exception {
        return mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(title, content, category, isPublished)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long createArticleAndGetId(User author, String title, String content,
                                       String category, Boolean isPublished) throws Exception {
        return objectMapper.readTree(
                createArticle(author, title, content, category, isPublished)
                        .getResponse().getContentAsString()).get("id").asLong();
    }
}
