package com.stratos.project;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.workspace.Workspace;
import com.stratos.workspace.WorkspaceService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceService workspaceService;

    @Transactional
    public ProjectResponse createProject(UUID workspaceId, ProjectRequest request) {
        Workspace workspace = workspaceService.findWorkspace(workspaceId);

        if (projectRepository.existsByWorkspaceIdAndProjectKey(workspace.getId(), request.getProjectKey())) {
            throw new IllegalArgumentException("A project with this key already exists in the workspace");
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setProjectKey(request.getProjectKey());
        project.setWorkspace(workspace);

        Project savedProject = projectRepository.save(project);
        return mapToResponse(savedProject);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectByKey(UUID workspaceId, String projectKey) {
        Project project = findByWorkspaceAndKey(workspaceId, projectKey);
        return mapToResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectsByWorkspace(UUID workspaceId) {
        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        return projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse updateProject(UUID workspaceId, String projectKey, ProjectRequest request) {
        Project project = findByWorkspaceAndKey(workspaceId, projectKey);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            project.setName(request.getName());
        }

        if (request.getProjectKey() != null && !request.getProjectKey().trim().isEmpty()
                && !project.getProjectKey().equals(request.getProjectKey())) {

            if (projectRepository.existsByWorkspaceIdAndProjectKey(project.getWorkspace().getId(),
                    request.getProjectKey())) {
                throw new IllegalArgumentException("A project with this key already exists in the workspace");
            }
            project.setProjectKey(request.getProjectKey());
        }

        Project updatedProject = projectRepository.save(project);
        return mapToResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(UUID workspaceId, String projectKey) {
        Project project = findByWorkspaceAndKey(workspaceId, projectKey);
        projectRepository.delete(project);
    }

    /**
     * Resolve a project by workspace ID and project key.
     */
    public Project findByWorkspaceAndKey(UUID workspaceId, String projectKey) {
        return projectRepository.findByWorkspaceIdAndProjectKey(workspaceId, projectKey)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with key: " + projectKey));
    }

    /**
     * Resolve a project by its UUID (used internally by TaskService).
     */
    public Project findProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));
    }

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                project.getWorkspace().getId());
    }
}
