package com.stratos.workspace;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.payload.response.WorkspaceResponse;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceResponse createWorkspace(Long userId, WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());
        workspace.setOwner(user);

        Workspace savedWorkspace = workspaceRepository.save(workspace);
        return mapToResponse(savedWorkspace);
    }

    public WorkspaceResponse getWorkspaceById(Long id) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
        return mapToResponse(workspace);
    }

    public List<WorkspaceResponse> getAllWorkspacesByUserId(Long userId) {
        List<Workspace> workspaces = workspaceRepository.findByOwnerId(userId);
        return workspaces.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(Long id, WorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workspace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        return mapToResponse(updatedWorkspace);
    }

    @Transactional
    public void deleteWorkspace(Long id) {
        if (!workspaceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Workspace not found with id: " + id);
        }
        workspaceRepository.deleteById(id);
    }

    private WorkspaceResponse mapToResponse(Workspace workspace) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getOwner().getId());
    }
}
