package com.stratos.unit.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.project.ProjectService;
import com.stratos.user.User;
import com.stratos.workspace.Workspace;
import com.stratos.workspace.WorkspaceRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private ProjectService projectService;

    private Workspace workspace;
    private Project project;

    @BeforeEach
    void setUp() {
        User owner = new User("owner", "owner@stratos.com", "hashed");
        owner.setId(1L);

        workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Test WS");
        workspace.setProjectCounter(0L);
        workspace.setOwner(owner);

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");
        project.setProjectKey("TST");
        project.setProjectNumber(1L);
        project.setTaskCounter(0L);
        project.setWorkspace(workspace);
    }

    // ========================================================================
    // createProject
    // ========================================================================

    @Nested
    class CreateProject {

        @Test
        void shouldCreateProjectWithScopedNumber() {
            ProjectRequest request = new ProjectRequest();
            request.setName("New Project");
            request.setProjectKey("NEW");

            when(workspaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(workspace));
            when(projectRepository.existsByWorkspaceIdAndProjectKey(1L, "NEW")).thenReturn(false);
            when(workspaceRepository.save(workspace)).thenReturn(workspace);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
                Project saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            ProjectResponse response = projectService.createProject(1L, request);

            assertThat(response.getName()).isEqualTo("New Project");
            assertThat(response.getProjectKey()).isEqualTo("NEW");
            assertThat(response.getProjectNumber()).isEqualTo(1L);
            assertThat(response.getWorkspaceId()).isEqualTo(1L);
        }

        @Test
        void shouldIncrementWorkspaceCounter() {
            assertThat(workspace.getProjectCounter()).isEqualTo(0L);

            ProjectRequest request = new ProjectRequest();
            request.setName("Counter Project");
            request.setProjectKey("CNT");

            when(workspaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(workspace));
            when(projectRepository.existsByWorkspaceIdAndProjectKey(1L, "CNT")).thenReturn(false);
            when(workspaceRepository.save(workspace)).thenReturn(workspace);
            when(projectRepository.save(any(Project.class))).thenAnswer(i -> {
                Project saved = i.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            projectService.createProject(1L, request);

            ArgumentCaptor<Workspace> wsCaptor = ArgumentCaptor.forClass(Workspace.class);
            verify(workspaceRepository).save(wsCaptor.capture());
            assertThat(wsCaptor.getValue().getProjectCounter()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenWorkspaceNotFound() {
            ProjectRequest request = new ProjectRequest();
            request.setName("No WS");
            request.setProjectKey("NWS");

            when(workspaceRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.createProject(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(projectRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenDuplicateKeyInSameWorkspace() {
            ProjectRequest request = new ProjectRequest();
            request.setName("Duplicate Key");
            request.setProjectKey("DUP");

            when(workspaceRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(workspace));
            when(projectRepository.existsByWorkspaceIdAndProjectKey(1L, "DUP")).thenReturn(true);

            assertThatThrownBy(() -> projectService.createProject(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("A project with this key already exists in the workspace");

            verify(projectRepository, never()).save(any());
        }
    }

    // ========================================================================
    // getProjectByNumber
    // ========================================================================

    @Nested
    class GetProjectByNumber {

        @Test
        void shouldReturnMappedResponse() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));

            ProjectResponse response = projectService.getProjectByNumber(1L, 1L);

            assertThat(response.getName()).isEqualTo("Test Project");
            assertThat(response.getProjectKey()).isEqualTo("TST");
            assertThat(response.getProjectNumber()).isEqualTo(1L);
            assertThat(response.getWorkspaceId()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectByNumber(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // getAllProjectsByWorkspace
    // ========================================================================

    @Nested
    class GetAllProjectsByWorkspace {

        @Test
        void shouldReturnMappedList() {
            Project project2 = new Project();
            project2.setId(11L);
            project2.setName("Project 2");
            project2.setProjectKey("PR2");
            project2.setProjectNumber(2L);
            project2.setWorkspace(workspace);

            when(workspaceRepository.existsById(1L)).thenReturn(true);
            when(projectRepository.findByWorkspaceId(1L)).thenReturn(List.of(project, project2));

            List<ProjectResponse> responses = projectService.getAllProjectsByWorkspace(1L);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getName()).isEqualTo("Test Project");
            assertThat(responses.get(1).getName()).isEqualTo("Project 2");
        }

        @Test
        void shouldReturnEmptyList() {
            when(workspaceRepository.existsById(1L)).thenReturn(true);
            when(projectRepository.findByWorkspaceId(1L)).thenReturn(Collections.emptyList());

            List<ProjectResponse> responses = projectService.getAllProjectsByWorkspace(1L);

            assertThat(responses).isEmpty();
        }

        @Test
        void shouldThrowWhenWorkspaceNotFound() {
            when(workspaceRepository.existsById(999L)).thenReturn(false);

            assertThatThrownBy(() -> projectService.getAllProjectsByWorkspace(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // deleteProject
    // ========================================================================

    @Nested
    class DeleteProject {

        @Test
        void shouldDeleteProject() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));

            projectService.deleteProject(1L, 1L);

            verify(projectRepository).delete(project);
        }

        @Test
        void shouldThrowWhenNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
