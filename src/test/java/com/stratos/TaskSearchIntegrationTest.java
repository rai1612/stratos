package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratos.payload.request.LoginRequest;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.request.SignupRequest;
import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long projectId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("search_user", "search@stratos.com");
        Long workspaceId = createWorkspace("Search WS");
        projectId = createProject("Search Project", "SRCH", workspaceId);

        // Seed Data
        createTask("Task 1", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now().plusDays(1));
        createTask("Task 2", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.now().plusDays(2));
        createTask("Task 3", TaskStatus.DONE, TaskPriority.MEDIUM, LocalDate.now().plusDays(3));
        createTask("Task 4", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(4));
    }

    @Test
    void shouldFilterTasksByStatus() throws Exception {
        mockMvc.perform(get("/api/tasks/search")
                .header("Authorization", "Bearer " + token)
                .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void shouldFilterTasksByPriority() throws Exception {
        mockMvc.perform(get("/api/tasks/search")
                .header("Authorization", "Bearer " + token)
                .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void shouldFilterTasksByDateRange() throws Exception {
        String start = LocalDate.now().plusDays(1).toString();
        String end = LocalDate.now().plusDays(2).toString();

        mockMvc.perform(get("/api/tasks/search")
                .header("Authorization", "Bearer " + token)
                .param("dueDateStart", start)
                .param("dueDateEnd", end))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void shouldPaginateResults() throws Exception {
        mockMvc.perform(get("/api/tasks/search")
                .header("Authorization", "Bearer " + token)
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    private void createTask(String title, TaskStatus status, TaskPriority priority, LocalDate dueDate)
            throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle(title);
        request.setProjectId(projectId);
        request.setStatus(status);
        request.setPriority(priority);
        request.setDueDate(dueDate);

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private Long createProject(String name, String key, Long workspaceId) throws Exception {
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
