package com.stratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private Long workspaceId;
    private Long projectNumber;
    private String projectKey;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("task_user", "task_user@stratos.com");
        workspaceId = createWorkspace("Task WS");
        projectKey = "TSK";
        projectNumber = createProject("Task Project", projectKey, workspaceId);
    }

    private String taskBasePath() {
        return "/api/workspaces/" + workspaceId + "/projects/" + projectNumber + "/tasks";
    }

    @Test
    void shouldCreateTaskWithScopedNumber() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("My Task");
        request.setDescription("Do something");
        request.setPriority(TaskPriority.HIGH);

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("My Task"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.taskNumber").value(1))
                .andExpect(jsonPath("$.taskKey").value(projectKey + "-1"));
    }

    @Test
    void shouldFailToCreateTaskWithPastDueDate() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Past Task");
        request.setDueDate(java.time.LocalDate.now().minusDays(1)); // Past date

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dueDate").value("Due date must be today or in the future"));
    }

    @Test
    void shouldFailToCreateTaskWithDoneStatus() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Done Task");
        request.setStatus(TaskStatus.DONE); // Not allowed on creation

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Task status cannot be DONE on creation"));
    }

    @Test
    void shouldAllowUpdatingTaskWithoutRequiredCreationFields() throws Exception {
        Long taskNumber = createTask("Validation Update Task");

        // Partial update: no title required on update
        TaskRequest updateRequest = new TaskRequest();
        updateRequest.setDescription("Updating only description");
        updateRequest.setStatus(TaskStatus.DONE); // DONE is allowed on update

        mockMvc.perform(put(taskBasePath() + "/" + taskNumber)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updating only description"))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void shouldUpdateTaskStatus() throws Exception {
        Long taskNumber = createTask("Status Task");

        mockMvc.perform(patch(taskBasePath() + "/" + taskNumber + "/status")
                .header("Authorization", "Bearer " + token)
                .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldDeleteTask() throws Exception {
        Long taskNumber = createTask("Delete Task");

        mockMvc.perform(delete(taskBasePath() + "/" + taskNumber)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(taskBasePath() + "/" + taskNumber)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAssignSequentialTaskNumbers() throws Exception {
        Long tn1 = createTask("Task 1");
        Long tn2 = createTask("Task 2");
        Long tn3 = createTask("Task 3");

        org.junit.jupiter.api.Assertions.assertEquals(1L, tn1);
        org.junit.jupiter.api.Assertions.assertEquals(2L, tn2);
        org.junit.jupiter.api.Assertions.assertEquals(3L, tn3);
    }

    /**
     * Creates a task and returns the taskNumber.
     */
    private Long createTask(String title) throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle(title);

        MvcResult result = mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("taskNumber").asLong();
    }

    private Long createProject(String name, String key, Long wsId) throws Exception {
        ProjectRequest request = new ProjectRequest();
        request.setName(name);
        request.setProjectKey(key);

        MvcResult result = mockMvc.perform(post("/api/workspaces/" + wsId + "/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("projectNumber").asLong();
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

        return login(username, "password123");
    }

    private String login(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }
}
