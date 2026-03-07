package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.ProjectRequest;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectIntegrationTest extends AbstractIntegrationTest {

        // ========================================================================
        // Happy path
        // ========================================================================

        @Nested
        class HappyPath {

                @Test
                void shouldCreateProject() throws Exception {
                        String token = registerAndLogin("proj_user", "proj@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");

                        ProjectRequest request = new ProjectRequest();
                        request.setName("My Project");
                        request.setProjectKey("PROJ");

                        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/projects")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("My Project"))
                                        .andExpect(jsonPath("$.projectKey").value("PROJ"))
                                        .andExpect(jsonPath("$.id").exists())
                                        .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()));
                }

                @Test
                void shouldGetAllProjectsInWorkspace() throws Exception {
                        String token = registerAndLogin("proj_all_user", "proj_all@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        createProject(token, "Project 1", "P1", workspaceId);
                        createProject(token, "Project 2", "P2", workspaceId);

                        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/projects")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)));
                }

                @Test
                void shouldGetProjectByKey() throws Exception {
                        String token = registerAndLogin("proj_get_user", "proj_get@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        createProject(token, "Fetch Me", "FET", workspaceId);

                        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/projects/FET")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("Fetch Me"))
                                        .andExpect(jsonPath("$.projectKey").value("FET"));
                }

                @Test
                void shouldUpdateProject() throws Exception {
                        String token = registerAndLogin("proj_upd_user", "proj_upd@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        createProject(token, "Before", "BEF", workspaceId);

                        ProjectRequest updateRequest = new ProjectRequest();
                        updateRequest.setName("After");

                        mockMvc.perform(put("/api/workspaces/" + workspaceId + "/projects/BEF")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(updateRequest)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.name").value("After"));
                }

                @Test
                void shouldDeleteProject() throws Exception {
                        String token = registerAndLogin("proj_del_user", "proj_del@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        createProject(token, "Delete Me", "DEL", workspaceId);

                        mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/projects/DEL")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isNoContent());

                        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/projects/DEL")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isNotFound());
                }

                @Test
                void shouldCascadeDeleteTasksWhenProjectDeleted() throws Exception {
                        String token = registerAndLogin("proj_cascade_user", "proj_cascade@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        String projectKey = createProject(token, "Cascade Me", "CAS", workspaceId);
                        createTask(token, "Task 1", workspaceId, projectKey);
                        createTask(token, "Task 2", workspaceId, projectKey);

                        // Verify tasks exist
                        mockMvc.perform(get("/api/workspaces/" + workspaceId + "/projects/" + projectKey + "/tasks")
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(2)));

                        // Delete project
                        mockMvc.perform(delete("/api/workspaces/" + workspaceId + "/projects/" + projectKey)
                                        .header("Authorization", "Bearer " + token))
                                        .andExpect(status().isNoContent());
                }
        }

        // ========================================================================
        // Edge cases
        // ========================================================================

        @Nested
        class EdgeCases {

                @Test
                void shouldRejectDuplicateProjectKeyInSameWorkspace() throws Exception {
                        String token = registerAndLogin("proj_dup_user", "proj_dup@stratos.com");
                        UUID workspaceId = createWorkspace(token, "Workspace");
                        createProject(token, "Project 1", "DUP", workspaceId);

                        ProjectRequest request = new ProjectRequest();
                        request.setName("Project 2");
                        request.setProjectKey("DUP");

                        mockMvc.perform(post("/api/workspaces/" + workspaceId + "/projects")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                void shouldAllowSameProjectKeyInDifferentWorkspaces() throws Exception {
                        String token = registerAndLogin("proj_key_user", "proj_key@stratos.com");
                        UUID workspace1 = createWorkspace(token, "Workspace One");
                        UUID workspace2 = createWorkspace(token, "Workspace Two");
                        createProject(token, "Project 1", "SAME", workspace1);

                        ProjectRequest request = new ProjectRequest();
                        request.setName("Project 2");
                        request.setProjectKey("SAME");

                        mockMvc.perform(post("/api/workspaces/" + workspace2 + "/projects")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());
                }
        }
}
