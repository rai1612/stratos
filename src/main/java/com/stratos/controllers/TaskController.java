package com.stratos.controllers;

import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.TaskSearchCriteria;
import com.stratos.validation.ValidationGroups;
import com.stratos.payload.response.TaskResponse;
import com.stratos.task.TaskService;
import com.stratos.task.TaskStatus;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects/{projectKey}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @Validated(ValidationGroups.Create.class) @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(workspaceId, projectKey, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskNumber}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<TaskResponse> getTask(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @PathVariable Long taskNumber) {
        TaskResponse response = taskService.getTask(workspaceId, projectKey, taskNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey) {
        List<TaskResponse> response = taskService.getTasksByProject(workspaceId, projectKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'VIEWER')")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            TaskSearchCriteria criteria,
            Pageable pageable) {
        Page<TaskResponse> response = taskService.searchTasks(workspaceId, projectKey, criteria, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{taskNumber}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @PathVariable Long taskNumber,
            @Validated(ValidationGroups.Update.class) @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(workspaceId, projectKey, taskNumber, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{taskNumber}/status")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @PathVariable Long taskNumber,
            @RequestParam TaskStatus status) {
        TaskResponse response = taskService.updateTaskStatus(workspaceId, projectKey, taskNumber, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskNumber}")
    @PreAuthorize("@workspaceSecurity.hasRole(authentication, #workspaceId, 'MEMBER')")
    public ResponseEntity<Void> deleteTask(
            @PathVariable UUID workspaceId,
            @PathVariable String projectKey,
            @PathVariable Long taskNumber) {
        taskService.deleteTask(workspaceId, projectKey, taskNumber);
        return ResponseEntity.noContent().build();
    }
}
