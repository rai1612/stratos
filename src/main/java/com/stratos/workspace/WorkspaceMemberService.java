package com.stratos.workspace;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.WorkspaceMemberRequest;
import com.stratos.payload.request.WorkspaceMemberUpdateRoleRequest;
import com.stratos.payload.response.WorkspaceInvitationResponse;
import com.stratos.payload.response.WorkspaceMemberResponse;
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
public class WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final com.stratos.task.TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getMembers(UUID workspaceId) {
        return workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> getInvitations(UUID workspaceId) {
        return workspaceInvitationRepository.findByWorkspaceId(workspaceId).stream()
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> getInvitationsForUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return workspaceInvitationRepository.findByEmail(user.getEmail()).stream()
                .filter(inv -> inv.getStatus() == InvitationStatus.PENDING)
                .map(this::mapToInvitationResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public WorkspaceInvitationResponse inviteUser(UUID workspaceId, UUID currentUserId,
            WorkspaceMemberRequest request) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
        User inviter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        WorkspaceRole targetRole = WorkspaceRole.valueOf(request.getRole());

        // Check if user is already a member
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
                throw new IllegalArgumentException("User is already a member of this workspace");
            }
        });

        // Check if pending invitation already exists
        workspaceInvitationRepository.findByWorkspaceIdAndEmail(workspaceId, request.getEmail())
                .ifPresent(existing -> {
                    if (existing.getStatus() == InvitationStatus.PENDING) {
                        throw new IllegalArgumentException("An invitation to this email is already pending");
                    }
                });

        WorkspaceInvitation invitation = new WorkspaceInvitation(workspace, request.getEmail(), targetRole, inviter);
        WorkspaceInvitation saved = workspaceInvitationRepository.save(invitation);

        // TODO: Send email ideally

        return mapToInvitationResponse(saved);
    }

    @Transactional
    public WorkspaceMemberResponse acceptInvitation(String token, UUID currentUserId) {
        WorkspaceInvitation invitation = workspaceInvitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is no longer valid");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(invitation.getEmail())) {
            throw new IllegalArgumentException("Invitation email does not match user email");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        workspaceInvitationRepository.save(invitation);

        WorkspaceMember member = new WorkspaceMember(invitation.getWorkspace(), user, invitation.getRole());
        WorkspaceMember saved = workspaceMemberRepository.save(member);

        return mapToMemberResponse(saved);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(UUID workspaceId, UUID targetUserId, UUID currentUserId,
            WorkspaceMemberUpdateRoleRequest request) {
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot change your own role.");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target member not found in workspace"));

        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current member not found in workspace"));

        WorkspaceRole newRole = WorkspaceRole.valueOf(request.getRole());

        if (newRole == WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Cannot promote to OWNER. Use the transfer ownership feature.");
        }

        if (currentMember.getRole() == WorkspaceRole.ADMIN) {
            if (targetMember.getRole() == WorkspaceRole.OWNER || targetMember.getRole() == WorkspaceRole.ADMIN) {
                throw new IllegalArgumentException("Admins cannot modify the roles of owners or other admins.");
            }
            if (newRole == WorkspaceRole.ADMIN) {
                throw new IllegalArgumentException("Only owners can promote members to Admin.");
            }
        }

        targetMember.setRole(newRole);
        WorkspaceMember saved = workspaceMemberRepository.save(targetMember);
        return mapToMemberResponse(saved);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID targetUserId, UUID currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot remove yourself. Use the leave workspace feature.");
        }

        WorkspaceMember targetMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target member not found in workspace"));

        WorkspaceMember currentMember = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current member not found in workspace"));

        if (currentMember.getRole() == WorkspaceRole.ADMIN) {
            if (targetMember.getRole() == WorkspaceRole.OWNER || targetMember.getRole() == WorkspaceRole.ADMIN) {
                throw new IllegalArgumentException("Admins cannot remove owners or other admins.");
            }
        }

        taskRepository.unassignTasksByWorkspaceAndAssignee(workspaceId, targetUserId);
        workspaceMemberRepository.delete(targetMember);
    }

    @Transactional
    public void leaveWorkspace(UUID workspaceId, UUID currentUserId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in workspace"));

        if (member.getRole() == WorkspaceRole.OWNER) {
            long ownerCount = workspaceMemberRepository.findByWorkspaceId(workspaceId).stream()
                    .filter(m -> m.getRole() == WorkspaceRole.OWNER)
                    .count();
            if (ownerCount <= 1) {
                throw new IllegalArgumentException(
                        "Cannot leave the workspace as the only owner. Transfer ownership first.");
            }
        }

        taskRepository.unassignTasksByWorkspaceAndAssignee(workspaceId, currentUserId);
        workspaceMemberRepository.delete(member);
    }

    @Transactional
    public void transferOwnership(UUID workspaceId, UUID newOwnerId, UUID currentUserId) {
        if (newOwnerId.equals(currentUserId)) {
            throw new IllegalArgumentException("You are already the owner.");
        }

        WorkspaceMember currentOwner = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current member not found"));

        if (currentOwner.getRole() != WorkspaceRole.OWNER) {
            throw new IllegalArgumentException("Only owners can transfer ownership.");
        }

        WorkspaceMember newOwner = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, newOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Target member not found in workspace"));

        newOwner.setRole(WorkspaceRole.OWNER);
        currentOwner.setRole(WorkspaceRole.ADMIN);

        workspaceMemberRepository.save(newOwner);
        workspaceMemberRepository.save(currentOwner);
    }

    private WorkspaceMemberResponse mapToMemberResponse(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getJoinedAt());
    }

    private WorkspaceInvitationResponse mapToInvitationResponse(WorkspaceInvitation invitation) {
        return new WorkspaceInvitationResponse(
                invitation.getId(),
                invitation.getWorkspace().getId(),
                invitation.getWorkspace().getName(),
                invitation.getEmail(),
                invitation.getToken(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getInvitedBy().getId(),
                invitation.getInvitedAt(),
                invitation.getExpiresAt());
    }
}
