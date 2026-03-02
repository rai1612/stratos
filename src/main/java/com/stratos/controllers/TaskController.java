package com.stratos.controllers;

import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.TaskSearchCriteria;
import com.stratos.payload.request.OnCreate;
import com.stratos.payload.request.OnUpdate;
import com.stratos.payload.response.TaskResponse;
import com.stratos.task.TaskService;
import com.stratos.task.TaskStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects/{projectNumber}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            @Validated(OnCreate.class) @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(workspaceId, projectNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{taskNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> getTaskByNumber(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            @PathVariable Long taskNumber) {
        TaskResponse response = taskService.getTaskByNumber(workspaceId, projectNumber, taskNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponse>> getTasksByProject(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber) {
        List<TaskResponse> response = taskService.getTasksByProject(workspaceId, projectNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            TaskSearchCriteria criteria,
            Pageable pageable) {
        Page<TaskResponse> response = taskService.searchTasks(workspaceId, projectNumber, criteria, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{taskNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            @PathVariable Long taskNumber,
            @Validated(OnUpdate.class) @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(workspaceId, projectNumber, taskNumber, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{taskNumber}/status")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            @PathVariable Long taskNumber,
            @RequestParam TaskStatus status) {
        TaskResponse response = taskService.updateTaskStatus(workspaceId, projectNumber, taskNumber, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{taskNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long workspaceId,
            @PathVariable Long projectNumber,
            @PathVariable Long taskNumber) {
        taskService.deleteTask(workspaceId, projectNumber, taskNumber);
        return ResponseEntity.noContent().build();
    }
}
