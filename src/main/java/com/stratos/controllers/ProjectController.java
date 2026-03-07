package com.stratos.controllers;

import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.project.ProjectService;
import com.stratos.validation.ValidationGroups;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID workspaceId,
            @Validated(ValidationGroups.Create.class) @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(workspaceId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectKey}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<ProjectResponse> getProjectByKey(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey) {
        ProjectResponse response = projectService.getProjectByKey(workspaceId, projectKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<List<ProjectResponse>> getAllProjectsByWorkspace(
            @PathVariable UUID workspaceId) {
        List<ProjectResponse> response = projectService.getAllProjectsByWorkspace(workspaceId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectKey}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @Validated(ValidationGroups.Update.class) @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.updateProject(workspaceId, projectKey, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectKey}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'ADMIN')")
    public ResponseEntity<Void> deleteProject(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey) {
        projectService.deleteProject(workspaceId, projectKey);
        return ResponseEntity.noContent().build();
    }
}
