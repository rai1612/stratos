package com.stratos.unit.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.payload.response.WorkspaceResponse;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import com.stratos.workspace.Workspace;
import com.stratos.workspace.WorkspaceRepository;
import com.stratos.workspace.WorkspaceService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = new User("owner", "owner@stratos.com", "hashed");
        owner.setId(1L);

        workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("Test WS");
        workspace.setDescription("A workspace");
        workspace.setOwner(owner);
        workspace.setProjectCounter(0L);
    }

    // ========================================================================
    // createWorkspace
    // ========================================================================

    @Nested
    class CreateWorkspace {

        @Test
        void shouldCreateWorkspace() {
            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("New WS");
            request.setDescription("New description");

            when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
            when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
                Workspace saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            WorkspaceResponse response = workspaceService.createWorkspace(1L, request);

            assertThat(response.getName()).isEqualTo("New WS");
            assertThat(response.getDescription()).isEqualTo("New description");
            assertThat(response.getOwnerId()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("No Owner WS");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.createWorkspace(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ========================================================================
    // getWorkspaceById
    // ========================================================================

    @Nested
    class GetWorkspaceById {

        @Test
        void shouldReturnMappedResponse() {
            when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));

            WorkspaceResponse response = workspaceService.getWorkspaceById(10L);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getName()).isEqualTo("Test WS");
            assertThat(response.getDescription()).isEqualTo("A workspace");
            assertThat(response.getOwnerId()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(workspaceRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.getWorkspaceById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // getAllWorkspacesByUserId
    // ========================================================================

    @Nested
    class GetAllWorkspacesByUserId {

        @Test
        void shouldReturnMappedList() {
            Workspace workspace2 = new Workspace();
            workspace2.setId(11L);
            workspace2.setName("WS 2");
            workspace2.setDescription("Second");
            workspace2.setOwner(owner);

            when(workspaceRepository.findByOwnerId(1L))
                    .thenReturn(List.of(workspace, workspace2));

            List<WorkspaceResponse> responses = workspaceService.getAllWorkspacesByUserId(1L);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getName()).isEqualTo("Test WS");
            assertThat(responses.get(1).getName()).isEqualTo("WS 2");
        }

        @Test
        void shouldReturnEmptyList() {
            when(workspaceRepository.findByOwnerId(1L))
                    .thenReturn(Collections.emptyList());

            List<WorkspaceResponse> responses = workspaceService.getAllWorkspacesByUserId(1L);

            assertThat(responses).isEmpty();
        }
    }

    // ========================================================================
    // deleteWorkspace
    // ========================================================================

    @Nested
    class DeleteWorkspace {

        @Test
        void shouldDeleteWorkspace() {
            when(workspaceRepository.existsById(10L)).thenReturn(true);

            workspaceService.deleteWorkspace(10L);

            verify(workspaceRepository).deleteById(10L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(workspaceRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> workspaceService.deleteWorkspace(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
