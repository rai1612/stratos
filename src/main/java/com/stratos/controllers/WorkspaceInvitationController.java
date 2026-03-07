package com.stratos.controllers;

import com.stratos.payload.response.WorkspaceMemberResponse;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces/invitations")
@RequiredArgsConstructor
public class WorkspaceInvitationController {

    private final WorkspaceMemberService workspaceMemberService;

    @PostMapping("/{token}/accept")
    public ResponseEntity<WorkspaceMemberResponse> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        WorkspaceMemberResponse response = workspaceMemberService.acceptInvitation(token, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
