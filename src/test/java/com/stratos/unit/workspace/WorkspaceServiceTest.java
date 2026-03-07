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
import com.stratos.workspace.WorkspaceMember;
import com.stratos.workspace.WorkspaceMemberRepository;
import com.stratos.workspace.WorkspaceRepository;
import com.stratos.workspace.WorkspaceRole;
import com.stratos.workspace.WorkspaceService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkspaceService workspaceService;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private User createTestUser() {
        User user = new User("testuser", "test@stratos.com", "hashed");
        user.setId(USER_ID);
        return user;
    }

    private Workspace createTestWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setName("Test Workspace");
        workspace.setDescription("Test Description");
        return workspace;
    }

    @Nested
    class CreateWorkspace {

        @Test
        void shouldCreateWorkspaceSuccessfully() {
            User user = createTestUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> {
                Workspace ws = invocation.getArgument(0);
                ws.setId(WORKSPACE_ID);
                return ws;
            });

            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("New Workspace");
            request.setDescription("Desc");

            WorkspaceResponse response = workspaceService.createWorkspace(USER_ID, request);

            assertThat(response.getName()).isEqualTo("New Workspace");
            assertThat(response.getRole()).isEqualTo(WorkspaceRole.OWNER);
        }

        @Test
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            WorkspaceRequest request = new WorkspaceRequest();
            request.setName("New Workspace");

            assertThatThrownBy(() -> workspaceService.createWorkspace(USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    class GetWorkspace {

        @Test
        void shouldGetWorkspaceById() {
            Workspace workspace = createTestWorkspace();
            when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));

            WorkspaceMember member = new WorkspaceMember(workspace, createTestUser(), WorkspaceRole.OWNER);
            when(workspaceMemberRepository.findByWorkspaceIdAndUserId(WORKSPACE_ID, USER_ID))
                    .thenReturn(Optional.of(member));

            WorkspaceResponse response = workspaceService.getWorkspaceById(WORKSPACE_ID, USER_ID);

            assertThat(response.getId()).isEqualTo(WORKSPACE_ID);
            assertThat(response.getName()).isEqualTo("Test Workspace");
        }

        @Test
        void shouldThrowWhenWorkspaceNotFound() {
            when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.getWorkspaceById(WORKSPACE_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workspace not found");
        }
    }

    @Nested
    class GetAllWorkspaces {

        @Test
        void shouldReturnAllWorkspacesForUser() {
            Workspace workspace = createTestWorkspace();
            WorkspaceMember member = new WorkspaceMember(workspace, createTestUser(), WorkspaceRole.OWNER);
            when(workspaceMemberRepository.findByUserId(USER_ID)).thenReturn(List.of(member));

            List<WorkspaceResponse> responses = workspaceService.getAllWorkspacesByUserId(USER_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getName()).isEqualTo("Test Workspace");
        }

        @Test
        void shouldReturnEmptyListWhenNoWorkspaces() {
            when(workspaceMemberRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<WorkspaceResponse> responses = workspaceService.getAllWorkspacesByUserId(USER_ID);

            assertThat(responses).isEmpty();
        }
    }

    @Nested
    class DeleteWorkspace {

        @Test
        void shouldDeleteWorkspaceSuccessfully() {
            Workspace workspace = createTestWorkspace();
            when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));

            workspaceService.deleteWorkspace(WORKSPACE_ID);

            verify(workspaceRepository).delete(workspace);
        }

        @Test
        void shouldThrowWhenDeletingNonExistentWorkspace() {
            when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workspaceService.deleteWorkspace(WORKSPACE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Workspace not found");
        }
    }
}
