package com.stratos.project;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.workspace.Workspace;
import com.stratos.workspace.WorkspaceRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public ProjectResponse createProject(Long workspaceId, ProjectRequest request) {
        // Lock workspace for safe counter increment
        Workspace workspace = workspaceRepository.findByIdForUpdate(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workspace not found with id: " + workspaceId));

        if (projectRepository.existsByWorkspaceIdAndProjectKey(workspaceId, request.getProjectKey())) {
            throw new IllegalArgumentException("A project with this key already exists in the workspace");
        }

        // Increment counter and assign scoped number
        workspace.setProjectCounter(workspace.getProjectCounter() + 1);
        workspaceRepository.save(workspace);

        Project project = new Project();
        project.setName(request.getName());
        project.setProjectKey(request.getProjectKey());
        project.setProjectNumber(workspace.getProjectCounter());
        project.setWorkspace(workspace);

        Project savedProject = projectRepository.save(project);
        return mapToResponse(savedProject);
    }

    public ProjectResponse getProjectByNumber(Long workspaceId, Long projectNumber) {
        Project project = projectRepository.findByWorkspaceIdAndProjectNumber(workspaceId, projectNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: workspace=" + workspaceId + ", projectNumber=" + projectNumber));
        return mapToResponse(project);
    }

    public List<ProjectResponse> getAllProjectsByWorkspace(Long workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace not found with id: " + workspaceId);
        }
        List<Project> projects = projectRepository.findByWorkspaceId(workspaceId);
        return projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectResponse updateProject(Long workspaceId, Long projectNumber, ProjectRequest request) {
        Project project = projectRepository.findByWorkspaceIdAndProjectNumber(workspaceId, projectNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: workspace=" + workspaceId + ", projectNumber=" + projectNumber));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            project.setName(request.getName());
        }

        if (request.getProjectKey() != null && !request.getProjectKey().trim().isEmpty()
                && !project.getProjectKey().equals(request.getProjectKey())) {

            if (projectRepository.existsByWorkspaceIdAndProjectKey(workspaceId, request.getProjectKey())) {
                throw new IllegalArgumentException("A project with this key already exists in the workspace");
            }
            project.setProjectKey(request.getProjectKey());
        }

        Project updatedProject = projectRepository.save(project);
        return mapToResponse(updatedProject);
    }

    @Transactional
    public void deleteProject(Long workspaceId, Long projectNumber) {
        Project project = projectRepository.findByWorkspaceIdAndProjectNumber(workspaceId, projectNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: workspace=" + workspaceId + ", projectNumber=" + projectNumber));
        projectRepository.delete(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                project.getProjectNumber(),
                project.getWorkspace().getId());
    }
}
