package com.stratos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.SignupRequest;
import java.util.Set;
import java.util.UUID;
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

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("stratos_test")
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

    protected String registerAndLogin(String username, String email) throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword("password123");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword("password123");

        MvcResult result = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    /**
     * Create a workspace and return its UUID id.
     */
    protected UUID createWorkspace(String token, String name) throws Exception {
        var request = new com.stratos.payload.request.WorkspaceRequest();
        request.setName(name);

        MvcResult result = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int statusCode = result.getResponse().getStatus();
        if (statusCode != 200) {
            throw new AssertionError("createWorkspace failed with status " + statusCode + ": " + responseBody);
        }
        String id = objectMapper.readTree(responseBody).get("id").asText();
        return UUID.fromString(id);
    }

    /**
     * Create a project and return its project key.
     */
    protected String createProject(String token, String name, String projectKey, UUID workspaceId)
            throws Exception {
        var request = new com.stratos.payload.request.ProjectRequest();
        request.setName(name);
        request.setProjectKey(projectKey);

        String url = "/api/workspaces/" + workspaceId + "/projects";
        MvcResult result = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int statusCode = result.getResponse().getStatus();
        if (statusCode != 200) {
            throw new AssertionError("createProject failed with status " + statusCode + ": " + responseBody);
        }
        return objectMapper.readTree(responseBody).get("projectKey").asText();
    }

    /**
     * Create a task and return its task number.
     */
    protected Long createTask(String token, String title, UUID workspaceId, String projectKey)
            throws Exception {
        var request = new com.stratos.payload.request.TaskRequest();
        request.setTitle(title);

        String url = "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks";
        MvcResult result = mockMvc
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        int statusCode = result.getResponse().getStatus();
        if (statusCode != 200) {
            throw new AssertionError("createTask failed with status " + statusCode + ": " + responseBody);
        }
        return objectMapper.readTree(responseBody).get("taskNumber").asLong();
    }
}
