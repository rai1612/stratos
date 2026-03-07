package com.stratos.controllers;

import com.stratos.payload.response.WorkspaceInvitationResponse;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceMemberService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserMeController {

    private final WorkspaceMemberService workspaceMemberService;

    @GetMapping("/invitations")
    public ResponseEntity<List<WorkspaceInvitationResponse>> getMyInvitations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<WorkspaceInvitationResponse> response = workspaceMemberService.getInvitationsForUser(userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
