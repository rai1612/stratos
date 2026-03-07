package com.stratos.security;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceMemberRepository;
import com.stratos.workspace.WorkspaceRepository;
import com.stratos.workspace.WorkspaceRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("workspaceSecurity")
@RequiredArgsConstructor
public class WorkspaceSecurityEvaluator {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;

    public boolean hasRole(Authentication authentication, UUID workspaceId, String requiredRoleStr) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new ResourceNotFoundException("Workspace not found with id: " + workspaceId);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (!(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return false;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UUID userId = userDetails.getId();

        WorkspaceRole requiredRole;
        try {
            requiredRole = WorkspaceRole.valueOf(requiredRoleStr);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(member -> canSatisfyRole(member.getRole(), requiredRole))
                .orElse(false);
    }

    private boolean canSatisfyRole(WorkspaceRole memberRole, WorkspaceRole requiredRole) {
        if (memberRole == WorkspaceRole.OWNER)
            return true;

        switch (requiredRole) {
            case OWNER:
                return false; // handled above
            case ADMIN:
                return memberRole == WorkspaceRole.ADMIN;
            case MEMBER:
                return memberRole == WorkspaceRole.ADMIN || memberRole == WorkspaceRole.MEMBER;
            case VIEWER:
                return memberRole == WorkspaceRole.ADMIN || memberRole == WorkspaceRole.MEMBER
                        || memberRole == WorkspaceRole.VIEWER;
            default:
                return false;
        }
    }
}
