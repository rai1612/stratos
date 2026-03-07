package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.TaskRequest;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TaskSearchIntegrationTest extends AbstractIntegrationTest {

        private String token;
        private UUID workspaceId;
        private String projectKey;

        @BeforeEach
        void setUp() throws Exception {
                token = registerAndLogin("search_user", "search@stratos.com");
                workspaceId = createWorkspace(token, "Search Workspace");
                projectKey = createProject(token, "Search Project", "SRCH", workspaceId);

                String taskBasePath = "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks";

                // Seed Data
                createTaskWithDetails("Task 1", TaskStatus.TODO, TaskPriority.LOW, LocalDate.now().plusDays(1));
                createTaskWithDetails("Task 2", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, LocalDate.now().plusDays(2));

                // Create task 3 as IN_PROGRESS then update to DONE
                Long task3Num = createTaskWithDetailsAndGetNumber("Task 3", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM,
                                LocalDate.now().plusDays(3));
                mockMvc.perform(patch(taskBasePath + "/" + task3Num + "/status")
                                .header("Authorization", "Bearer " + token)
                                .param("status", "DONE"))
                                .andExpect(status().isOk());

                createTaskWithDetails("Task 4", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.now().plusDays(4));
        }

        private String searchPath() {
                return "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks/search";
        }

        @Test
        void shouldFilterTasksByStatus() throws Exception {
                mockMvc.perform(get(searchPath())
                                .header("Authorization", "Bearer " + token)
                                .param("status", "TODO"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        void shouldFilterTasksByPriority() throws Exception {
                mockMvc.perform(get(searchPath())
                                .header("Authorization", "Bearer " + token)
                                .param("priority", "HIGH"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        void shouldFilterTasksByDateRange() throws Exception {
                String start = LocalDate.now().plusDays(1).toString();
                String end = LocalDate.now().plusDays(2).toString();

                mockMvc.perform(get(searchPath())
                                .header("Authorization", "Bearer " + token)
                                .param("dueDateStart", start)
                                .param("dueDateEnd", end))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        void shouldPaginateResults() throws Exception {
                mockMvc.perform(get(searchPath())
                                .header("Authorization", "Bearer " + token)
                                .param("page", "0")
                                .param("size", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.page.totalElements").value(4));
        }

        private void createTaskWithDetails(String title, TaskStatus status, TaskPriority priority, LocalDate dueDate)
                        throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle(title);
                request.setStatus(status);
                request.setPriority(priority);
                request.setDueDate(dueDate);

                String taskPath = "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks";
                mockMvc.perform(post(taskPath)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        private Long createTaskWithDetailsAndGetNumber(String title, TaskStatus status, TaskPriority priority,
                        LocalDate dueDate) throws Exception {
                TaskRequest request = new TaskRequest();
                request.setTitle(title);
                request.setStatus(status);
                request.setPriority(priority);
                request.setDueDate(dueDate);

                String taskPath = "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks";
                MvcResult result = mockMvc.perform(post(taskPath)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andReturn();

                return objectMapper.readTree(result.getResponse().getContentAsString()).get("taskNumber").asLong();
        }
}
