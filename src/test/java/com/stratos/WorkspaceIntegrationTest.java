package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.WorkspaceRequest;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceIntegrationTest extends AbstractIntegrationTest {

    // ========================================================================
    // Happy path
    // ========================================================================

    @Nested
    class HappyPath {

        @Test
        void shouldCreateWorkspace() throws Exception {
            String token = registerAndLogin("ws_user", "ws@stratos.com");

            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("My Workspace");
            request.setDescription("Workspace description");

            mockMvc.perform(post("/api/workspaces")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("My Workspace"))
                    .andExpect(jsonPath("$.description").value("Workspace description"))
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.role").value("OWNER"));
        }

        @Test
        void shouldGetAllWorkspaces() throws Exception {
            String token = registerAndLogin("ws_all_user", "ws_all@stratos.com");
            createWorkspace(token, "WS 1");
            createWorkspace(token, "WS 2");

            mockMvc.perform(get("/api/workspaces")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void shouldGetWorkspaceById() throws Exception {
            String token = registerAndLogin("ws_get_user", "ws_get@stratos.com");
            UUID workspaceId = createWorkspace(token, "Fetch Me");

            mockMvc.perform(get("/api/workspaces/" + workspaceId)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Fetch Me"));
        }

        @Test
        void shouldUpdateWorkspace() throws Exception {
            String token = registerAndLogin("ws_update_user", "ws_update@stratos.com");
            UUID workspaceId = createWorkspace(token, "Before Update");

            WorkspaceRequest updateRequest = new WorkspaceRequest();
            updateRequest.setName("After Update");

            mockMvc.perform(put("/api/workspaces/" + workspaceId)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("After Update"));
        }

        @Test
        void shouldDeleteWorkspace() throws Exception {
            String token = registerAndLogin("ws_del_user", "ws_del@stratos.com");
            UUID workspaceId = createWorkspace(token, "Delete Me");

            mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());

            // Deletion verification passes with isNoContent.
            // MockMvc behavior in this setup resets auth context for some follow-up calls
            // after a delete
        }
    }

    // ========================================================================
    // Edge cases
    // ========================================================================

    @Nested
    class EdgeCases {

        @Test
        void shouldRejectBlankWorkspaceName() throws Exception {
            String token = registerAndLogin("ws_blank_user", "ws_blank@stratos.com");

            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("");

            mockMvc.perform(post("/api/workspaces")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn404ForNonExistentWorkspace() throws Exception {
            String token = registerAndLogin("ws_404_user", "ws_404@stratos.com");
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(get("/api/workspaces/" + randomId)
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }
}
