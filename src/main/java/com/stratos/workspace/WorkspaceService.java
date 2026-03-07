package com.stratos.workspace;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.payload.response.WorkspaceResponse;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkspaceResponse createWorkspace(UUID userId, WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Workspace workspace = new Workspace();
        workspace.setName(request.getName());
        workspace.setDescription(request.getDescription());

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember(savedWorkspace, user, WorkspaceRole.OWNER);
        workspaceMemberRepository.save(member);

        return mapToResponse(savedWorkspace, WorkspaceRole.OWNER);
    }

    public WorkspaceResponse getWorkspaceById(UUID workspaceId, UUID userId) {
        Workspace workspace = findWorkspace(workspaceId);
        WorkspaceRole role = getRoleForUser(workspaceId, userId);
        return mapToResponse(workspace, role);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getAllWorkspacesByUserId(UUID userId) {
        List<WorkspaceMember> members = workspaceMemberRepository.findByUserId(userId);
        return members.stream()
                .map(member -> mapToResponse(member.getWorkspace(), member.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceResponse updateWorkspace(UUID workspaceId, WorkspaceRequest request, UUID userId) {
        Workspace workspace = findWorkspace(workspaceId);

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            workspace.setName(request.getName());
        }
        if (request.getDescription() != null) {
            workspace.setDescription(request.getDescription());
        }

        Workspace updatedWorkspace = workspaceRepository.save(workspace);
        WorkspaceRole role = getRoleForUser(workspaceId, userId);
        return mapToResponse(updatedWorkspace, role);
    }

    @Transactional
    public void deleteWorkspace(UUID workspaceId) {
        Workspace workspace = findWorkspace(workspaceId);
        workspaceRepository.delete(workspace);
    }

    /**
     * Resolve a workspace by its ID.
     * Also used by other services.
     */
    public Workspace findWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Workspace not found with id: " + workspaceId));
    }

    private WorkspaceRole getRoleForUser(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElse(null);
    }

    private WorkspaceResponse mapToResponse(Workspace workspace, WorkspaceRole role) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                role);
    }
}
