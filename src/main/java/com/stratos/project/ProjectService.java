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
    public ProjectResponse createProject(ProjectRequest request) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workspace not found with id: " + request.getWorkspaceId()));

        Project project = new Project();
        project.setName(request.getName());
        project.setProjectKey(request.getProjectKey());
        project.setWorkspace(workspace);

        Project savedProject = projectRepository.save(project);
        return mapToResponse(savedProject);
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
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
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    private ProjectResponse mapToResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getProjectKey(),
                project.getWorkspace().getId());
    }
}
