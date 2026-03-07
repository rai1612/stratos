package com.stratos.controllers;

import com.stratos.payload.request.WorkspaceMemberRequest;
import com.stratos.payload.request.WorkspaceMemberUpdateRoleRequest;
import com.stratos.payload.response.WorkspaceInvitationResponse;
import com.stratos.payload.response.WorkspaceMemberResponse;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceMemberService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces/{workspaceId}")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    @GetMapping("/members")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<List<WorkspaceMemberResponse>> getMembers(@PathVariable UUID workspaceId) {
        List<WorkspaceMemberResponse> response = workspaceMemberService.getMembers(workspaceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/invitations")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<List<WorkspaceInvitationResponse>> getInvitations(@PathVariable UUID workspaceId) {
        List<WorkspaceInvitationResponse> response = workspaceMemberService.getInvitations(workspaceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invitations")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<WorkspaceInvitationResponse> inviteUser(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WorkspaceMemberRequest request) {
        WorkspaceInvitationResponse response = workspaceMemberService.inviteUser(workspaceId, userDetails.getId(),
                request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/members/{userId}/role")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WorkspaceMemberUpdateRoleRequest request) {
        WorkspaceMemberResponse response = workspaceMemberService.updateMemberRole(workspaceId, userId,
                userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/members/{userId}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceMemberService.removeMember(workspaceId, userId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/leave")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<Void> leaveWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceMemberService.leaveWorkspace(workspaceId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/transfer-ownership/{newOwnerId}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'OWNER')")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable UUID workspaceId,
            @PathVariable UUID newOwnerId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        workspaceMemberService.transferOwnership(workspaceId, newOwnerId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
