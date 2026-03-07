package com.stratos.controllers;

import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.payload.response.WorkspaceResponse;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceService;
import com.stratos.validation.ValidationGroups;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Validated(ValidationGroups.Create.class) @RequestBody WorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.createWorkspace(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<WorkspaceResponse> response = workspaceService.getAllWorkspacesByUserId(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID workspaceId) {
        WorkspaceResponse response = workspaceService.getWorkspaceById(workspaceId, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable UUID workspaceId,
            @Validated(ValidationGroups.Update.class) @RequestBody WorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.updateWorkspace(workspaceId, request, userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{workspaceId}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'OWNER')")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable UUID workspaceId) {
        workspaceService.deleteWorkspace(workspaceId);
        return ResponseEntity.noContent().build();
    }
}
