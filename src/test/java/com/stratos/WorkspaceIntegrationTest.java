package com.stratos;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.stratos.payload.request.WorkspaceRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceIntegrationTest extends AbstractIntegrationTest {

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("workspace_user", "ws_user@stratos.com");
    }

    @Test
    void shouldCreateWorkspace() throws Exception {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("My Workspace");
        request.setDescription("Test Description");

        mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("My Workspace"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    void shouldGetAllWorkspacesForUser() throws Exception {
        createWorkspace(token, "WS 1");
        createWorkspace(token, "WS 2");

        mockMvc.perform(get("/api/workspaces")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldGetWorkspaceById() throws Exception {
        Long workspaceId = createWorkspace(token, "Target WS");

        mockMvc.perform(get("/api/workspaces/" + workspaceId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Target WS"));
    }

    @Test
    void shouldDeleteWorkspace() throws Exception {
        Long workspaceId = createWorkspace(token, "To Delete");

        mockMvc.perform(delete("/api/workspaces/" + workspaceId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/workspaces/" + workspaceId)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        mockMvc.perform(get("/api/workspaces/999999")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ========================================================================
    // Validation edge cases
    // ========================================================================

    @Test
    void shouldRejectBlankWorkspaceName() throws Exception {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName(""); // @NotBlank

        mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectWorkspaceNameTooLong() throws Exception {
        WorkspaceRequest request = new WorkspaceRequest();
        request.setName("a".repeat(51)); // @Size(max = 50)

        mockMvc.perform(post("/api/workspaces")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
