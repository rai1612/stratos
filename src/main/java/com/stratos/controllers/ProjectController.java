package com.stratos.controllers;

import com.stratos.payload.request.ProjectRequest;
import com.stratos.payload.response.ProjectResponse;
import com.stratos.project.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
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
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long workspaceId,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse response = projectService.createProject(workspaceId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ProjectResponse> getProjectByNumber(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber) {
        ProjectResponse response = projectService.getProjectByNumber(workspaceId, projectNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ProjectResponse>> getAllProjectsByWorkspace(@PathVariable Long workspaceId) {
        List<ProjectResponse> response = projectService.getAllProjectsByWorkspace(workspaceId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber) {
        projectService.deleteProject(workspaceId, projectNumber);
        return ResponseEntity.noContent().build();
    }
}
