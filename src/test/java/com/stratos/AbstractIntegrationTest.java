package com.stratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.request.SignupRequest;
import com.stratos.payload.request.WorkspaceRequest;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ========================================================================
    // Shared test helpers
    // ========================================================================

    /**
     * Registers a user and logs in, returning the JWT token.
     */
    protected String registerAndLogin(String username, String email) throws Exception {
        return registerAndLogin(username, email, null);
    }

    /**
     * Registers a user with optional roles and logs in, returning the JWT token.
     */
    protected String registerAndLogin(String username, String email, Set<String> roles)
            throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword("password123");
        signupRequest.setRole(roles);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    /**
     * Creates a workspace and returns its ID.
     */
    protected Long createWorkspace(String token, String name) throws Exception {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName(name);
        request.setDescription("Desc");

        MvcResult result = mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    /**
     * Creates a project and returns its projectNumber.
     */
    protected Long createProject(String token, String name, String key, Long workspaceId)
            throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName(name);
        request.setProjectKey(key);

        MvcResult result = mockMvc.perform(
                post("/api/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("projectNumber").asLong();
    }
}
