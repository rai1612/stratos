package com.stratos.task;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.TaskSearchCriteria;
import com.stratos.payload.response.TaskResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.project.ProjectService;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import com.stratos.workspace.WorkspaceMemberRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public TaskResponse createTask(UUID workspaceId, String projectKey, TaskRequest request) {
        Project project = projectService.findByWorkspaceAndKey(workspaceId, projectKey);

        if (request.getStatus() == TaskStatus.DONE) {
            throw new IllegalArgumentException("Task status cannot be DONE on creation");
        }

        // Lock project row and increment task counter
        Project lockedProject = projectRepository.findByIdForUpdate(project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        lockedProject.setTaskCounter(lockedProject.getTaskCounter() + 1);
        projectRepository.save(lockedProject);

        Task task = new Task();
        task.setTaskNumber(lockedProject.getTaskCounter());
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }
        task.setProject(project);

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("User not found with id: " + request.getAssigneeId()));
            if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getAssigneeId())) {
                throw new IllegalArgumentException("Assignee must be a member of the workspace");
            }
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID workspaceId, String projectKey, Long taskNumber) {
        Task task = findTask(workspaceId, projectKey, taskNumber);
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(UUID workspaceId, String projectKey) {
        Project project = projectService.findByWorkspaceAndKey(workspaceId, projectKey);
        return taskRepository.findByProjectId(project.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(UUID workspaceId, String projectKey,
            TaskSearchCriteria criteria, Pageable pageable) {
        Project project = projectService.findByWorkspaceAndKey(workspaceId, projectKey);
        Specification<Task> spec = TaskSpecification.getSpecification(project.getId(), criteria);
        return taskRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public TaskResponse updateTask(UUID workspaceId, String projectKey, Long taskNumber, TaskRequest request) {
        Task task = findTask(workspaceId, projectKey, taskNumber);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null)
            task.setStatus(request.getStatus());
        if (request.getPriority() != null)
            task.setPriority(request.getPriority());

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("User not found with id: " + request.getAssigneeId()));
            if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, request.getAssigneeId())) {
                throw new IllegalArgumentException("Assignee must be a member of the workspace");
            }
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null);
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(UUID workspaceId, String projectKey, Long taskNumber, TaskStatus status) {
        Task task = findTask(workspaceId, projectKey, taskNumber);
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(UUID workspaceId, String projectKey, Long taskNumber) {
        Task task = findTask(workspaceId, projectKey, taskNumber);
        taskRepository.delete(task);
    }

    private Task findTask(UUID workspaceId, String projectKey, Long taskNumber) {
        Project project = projectService.findByWorkspaceAndKey(workspaceId, projectKey);
        return taskRepository.findByProjectIdAndTaskNumber(project.getId(), taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: " + projectKey + "-" + taskNumber));
    }

    private TaskResponse mapToResponse(Task task) {
        java.time.LocalDate dueDate = task.getDueDate() != null
                ? java.time.LocalDate.ofInstant(task.getDueDate(), java.time.ZoneId.systemDefault())
                : null;

        String taskKey = task.getProject().getProjectKey() + "-" + task.getTaskNumber();

        return new TaskResponse(
                task.getId(),
                task.getTaskNumber(),
                taskKey,
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                dueDate,
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getUsername() : null,
                task.getProject().getId(),
                task.getProject().getName());
    }
}
