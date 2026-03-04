package com.stratos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.TaskRequest;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TaskIntegrationTest extends AbstractIntegrationTest {

    private String token;
    private Long workspaceId;
    private Long projectNumber;
    private String projectKey;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("task_user", "task_user@stratos.com");
        workspaceId = createWorkspace(token, "Task WS");
        projectKey = "TSK";
        projectNumber = createProject(token, "Task Project", projectKey, workspaceId);
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

    // ========================================================================
    // Validation edge cases
    // ========================================================================

    @Test
    void shouldRejectBlankTitle() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle(""); // @NotBlank

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTitleTooLong() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("a".repeat(101)); // @Size(max = 100)

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDescriptionTooLong() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("a".repeat(501)); // @Size(max = 500)

        mockMvc.perform(post(taskBasePath())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================================================================
    // Not-found edge cases
    // ========================================================================

    @Test
    void shouldReturn404ForNonExistentTask() throws Exception {
        mockMvc.perform(get(taskBasePath() + "/999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentTask() throws Exception {
        TaskRequest request = new TaskRequest();
        request.setTitle("Ghost Task");

        mockMvc.perform(put(taskBasePath() + "/999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenDeletingNonExistentTask() throws Exception {
        mockMvc.perform(delete(taskBasePath() + "/999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForTaskUnderNonExistentProject() throws Exception {
        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/projects/999/tasks/1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // Cross-entity isolation
    // ========================================================================

    @Test
    void shouldScopeTaskNumbersPerProject() throws Exception {
        // Create tasks in first project
        Long tn1 = createTask("Project1 Task1");
        Long tn2 = createTask("Project1 Task2");

        // Create a second project
        Long project2Number = createProject(token, "Second Project", "PRJ2", workspaceId);
        String project2TaskPath = "/api/workspaces/" + workspaceId + "/projects/" + project2Number + "/tasks";

        // Create task in second project — should start from 1
        TaskRequest request = new TaskRequest();
        request.setTitle("Project2 Task1");

        MvcResult result = mockMvc.perform(post(project2TaskPath)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long p2tn1 = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("taskNumber").asLong();

        // Project 1 tasks: 1, 2; Project 2 tasks: 1 (independent)
        org.junit.jupiter.api.Assertions.assertEquals(1L, tn1);
        org.junit.jupiter.api.Assertions.assertEquals(2L, tn2);
        org.junit.jupiter.api.Assertions.assertEquals(1L, p2tn1);
    }

    // ========================================================================
    // Helper
    // ========================================================================

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
}
