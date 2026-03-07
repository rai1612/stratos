package com.stratos;

import static org.hamcrest.Matchers.hasSize;
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
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class TaskIntegrationTest extends AbstractIntegrationTest {

    private String taskBasePath(UUID workspaceId, String projectKey) {
        return "/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks";
    }

    // ========================================================================
    // Happy path
    // ========================================================================

    @Nested
    class HappyPath {

        @Test
        void shouldCreateTaskWithSequentialNumber() throws Exception {
            String token = registerAndLogin("task_user", "task@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);

            TaskRequest request = new TaskRequest();
            request.setTitle("My Task");
            request.setStatus(TaskStatus.TODO);
            request.setPriority(TaskPriority.HIGH);

            mockMvc.perform(post(taskBasePath(wsId, projKey))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("My Task"))
                    .andExpect(jsonPath("$.taskNumber").value(1))
                    .andExpect(jsonPath("$.taskKey").value("PRJ-1"))
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));
        }

        @Test
        void shouldGetAllTasksInProject() throws Exception {
            String token = registerAndLogin("task_all_user", "task_all@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);
            createTask(token, "Task 1", wsId, projKey);
            createTask(token, "Task 2", wsId, projKey);

            mockMvc.perform(get(taskBasePath(wsId, projKey))
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void shouldGetTaskByNumber() throws Exception {
            String token = registerAndLogin("task_get_user", "task_get@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);
            Long taskNum = createTask(token, "Fetch Me", wsId, projKey);

            mockMvc.perform(get(taskBasePath(wsId, projKey) + "/" + taskNum)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Fetch Me"))
                    .andExpect(jsonPath("$.taskKey").value("PRJ-" + taskNum));
        }

        @Test
        void shouldUpdateTask() throws Exception {
            String token = registerAndLogin("task_upd_user", "task_upd@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);
            Long taskNum = createTask(token, "Before", wsId, projKey);

            TaskRequest updateRequest = new TaskRequest();
            updateRequest.setTitle("After");
            updateRequest.setStatus(TaskStatus.IN_PROGRESS);
            updateRequest.setPriority(TaskPriority.LOW);

            mockMvc.perform(put(taskBasePath(wsId, projKey) + "/" + taskNum)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("After"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        void shouldUpdateTaskStatus() throws Exception {
            String token = registerAndLogin("task_status_user", "task_status@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);
            Long taskNum = createTask(token, "Status Task", wsId, projKey);

            mockMvc.perform(patch(taskBasePath(wsId, projKey) + "/" + taskNum + "/status")
                    .header("Authorization", "Bearer " + token)
                    .param("status", "DONE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"));
        }

        @Test
        void shouldDeleteTask() throws Exception {
            String token = registerAndLogin("task_del_user", "task_del@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);
            Long taskNum = createTask(token, "Delete Me", wsId, projKey);

            mockMvc.perform(delete(taskBasePath(wsId, projKey) + "/" + taskNum)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get(taskBasePath(wsId, projKey) + "/" + taskNum)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Nested
    class EdgeCases {

        @Test
        void shouldRejectBlankTaskTitle() throws Exception {
            String token = registerAndLogin("task_blank_user", "task_blank@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);

            TaskRequest request = new TaskRequest();
            request.setTitle("");

            mockMvc.perform(post(taskBasePath(wsId, projKey))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectDoneStatusOnCreation() throws Exception {
            String token = registerAndLogin("task_done_user", "task_done@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);

            TaskRequest request = new TaskRequest();
            request.setTitle("Should Fail");
            request.setStatus(TaskStatus.DONE);

            mockMvc.perform(post(taskBasePath(wsId, projKey))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn404ForNonExistentTask() throws Exception {
            String token = registerAndLogin("task_404_user", "task_404@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);

            mockMvc.perform(get(taskBasePath(wsId, projKey) + "/999")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldDefaultToTodoAndMediumPriority() throws Exception {
            String token = registerAndLogin("task_default_user", "task_default@stratos.com");
            UUID wsId = createWorkspace(token, "Workspace");
            String projKey = createProject(token, "Proj", "PRJ", wsId);

            TaskRequest request = new TaskRequest();
            request.setTitle("Defaulted Task");

            mockMvc.perform(post(taskBasePath(wsId, projKey))
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("TODO"))
                    .andExpect(jsonPath("$.priority").value("MEDIUM"));
        }
    }
}
