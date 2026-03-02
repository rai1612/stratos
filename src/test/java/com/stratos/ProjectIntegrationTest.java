package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.request.SignupRequest;
import com.stratos.payload.request.WorkspaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long workspaceId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("project_user", "prj_user@stratos.com");
        workspaceId = createWorkspace("Project WS");
    }

    @Test
    void shouldCreateProject() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName("My Project");
        request.setProjectKey("PRJ");
        request.setWorkspaceId(workspaceId);

        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.projectKey").value("PRJ"));
    }

    @Test
    void shouldGetAllProjectsByWorkspace() throws Exception {
        createProject("Project A", "PRJA");
        createProject("Project B", "PRJB");

        mockMvc.perform(get("/api/projects/workspace/" + workspaceId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldDeleteProject() throws Exception {
        Long projectId = createProject("To Delete", "DEL");

        mockMvc.perform(delete("/api/projects/" + projectId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailToCreateProjectWithInvalidWorkspace() throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName("Invalid WS Project");
        request.setProjectKey("INV");
        request.setWorkspaceId(999999L);

        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFailToCreateDuplicateProjectKeyInSameWorkspace() throws Exception {
        // Create first project
        ProjectRequest request1 = new ProjectRequest();
        request1.setName("First Project");
        request1.setProjectKey("DUP");
        request1.setWorkspaceId(workspaceId);

        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Attempt to create second project with the same key in the same workspace
        ProjectRequest request2 = new ProjectRequest();
        request2.setName("Second Project");
        request2.setProjectKey("DUP");
        request2.setWorkspaceId(workspaceId);

        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A project with this key already exists in the workspace"));

        // Ensure that the same key can be used in a different workspace
        Long workspaceId2 = createWorkspace("Second WS");
        ProjectRequest request3 = new ProjectRequest();
        request3.setName("Third Project");
        request3.setProjectKey("DUP");
        request3.setWorkspaceId(workspaceId2);

        mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isOk());
    }

    private Long createProject(String name, String key) throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName(name);
        request.setProjectKey(key);
        request.setWorkspaceId(workspaceId);

        MvcResult result = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createWorkspace(String name) throws Exception {
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

    private String registerAndLogin(String username, String email) throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword("password123");

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
}
