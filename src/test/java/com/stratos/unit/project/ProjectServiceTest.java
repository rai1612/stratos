package com.stratos.unit.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.project.ProjectService;
import com.stratos.workspace.Workspace;
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
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private ProjectService projectService;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");

    private Workspace createTestWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setName("Test Workspace");
        return workspace;
    }

    private Project createTestProject(Workspace workspace) {
        Project project = new Project();
        project.setId(PROJECT_ID);
        project.setName("Test Project");
        project.setProjectKey("TEST");
        project.setWorkspace(workspace);
        return project;
    }

    @Nested
    class CreateProject {

        @Test
        void shouldCreateProjectSuccessfully() {
            Workspace workspace = createTestWorkspace();
            when(workspaceService.findWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(projectRepository.existsByWorkspaceIdAndProjectKey(WORKSPACE_ID, "NEW")).thenReturn(false);
            when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
                Project p = invocation.getArgument(0);
                p.setId(PROJECT_ID);
                return p;
            });

            ProjectRequest request = new ProjectRequest();
            request.setName("New Project");
            request.setProjectKey("NEW");

            ProjectResponse response = projectService.createProject(WORKSPACE_ID, request);

            assertThat(response.getName()).isEqualTo("New Project");
            assertThat(response.getProjectKey()).isEqualTo("NEW");
            assertThat(response.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        }

        @Test
        void shouldThrowWhenDuplicateProjectKey() {
            Workspace workspace = createTestWorkspace();
            when(workspaceService.findWorkspace(WORKSPACE_ID)).thenReturn(workspace);
            when(projectRepository.existsByWorkspaceIdAndProjectKey(WORKSPACE_ID, "DUP")).thenReturn(true);

            ProjectRequest request = new ProjectRequest();
            request.setName("Dup Project");
            request.setProjectKey("DUP");

            assertThatThrownBy(() -> projectService.createProject(WORKSPACE_ID, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    class GetProject {

        @Test
        void shouldGetProjectByKey() {
            Workspace workspace = createTestWorkspace();
            Project project = createTestProject(workspace);
            when(projectRepository.findByWorkspaceIdAndProjectKey(WORKSPACE_ID, "TEST"))
                    .thenReturn(Optional.of(project));

            ProjectResponse response = projectService.getProjectByKey(WORKSPACE_ID, "TEST");

            assertThat(response.getId()).isEqualTo(PROJECT_ID);
            assertThat(response.getName()).isEqualTo("Test Project");
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectKey(WORKSPACE_ID, "NOPE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.getProjectByKey(WORKSPACE_ID, "NOPE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found");
        }
    }

    @Nested
    class GetAllProjects {

        @Test
        void shouldReturnProjectsForWorkspace() {
            Workspace workspace = createTestWorkspace();
            Project project = createTestProject(workspace);
            when(projectRepository.findByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of(project));

            List<ProjectResponse> responses = projectService.getAllProjectsByWorkspace(WORKSPACE_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getName()).isEqualTo("Test Project");
        }
    }

    @Nested
    class DeleteProject {

        @Test
        void shouldDeleteProjectSuccessfully() {
            Workspace workspace = createTestWorkspace();
            Project project = createTestProject(workspace);
            when(projectRepository.findByWorkspaceIdAndProjectKey(WORKSPACE_ID, "TEST"))
                    .thenReturn(Optional.of(project));

            projectService.deleteProject(WORKSPACE_ID, "TEST");

            verify(projectRepository).delete(project);
        }

        @Test
        void shouldThrowWhenDeletingNonExistentProject() {
            when(projectRepository.findByWorkspaceIdAndProjectKey(WORKSPACE_ID, "NOPE"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.deleteProject(WORKSPACE_ID, "NOPE"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Project not found");
        }
    }
}
