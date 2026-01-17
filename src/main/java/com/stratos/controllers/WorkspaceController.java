package com.stratos.controllers;

import com.stratos.payload.request.WorkspaceRequest;
import com.stratos.payload.response.WorkspaceResponse;
import com.stratos.security.services.UserDetailsImpl;
import com.stratos.workspace.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
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
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WorkspaceRequest request) {
        WorkspaceResponse response = workspaceService.createWorkspace(userDetails.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<WorkspaceResponse> response = workspaceService.getAllWorkspacesByUserId(userDetails.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById(@PathVariable Long id) {
        WorkspaceResponse response = workspaceService.getWorkspaceById(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }
}
