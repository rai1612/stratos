package com.stratos.task;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.TaskSearchCriteria;
import com.stratos.payload.response.TaskResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import java.util.List;
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
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(Long workspaceId, Long projectNumber, TaskRequest request) {
        Project project = findProject(workspaceId, projectNumber);

        // Lock project for safe counter increment
        project = projectRepository.findByIdForUpdate(project.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (request.getStatus() == TaskStatus.DONE) {
            throw new IllegalArgumentException("Task status cannot be DONE on creation");
        }

        // Increment counter and assign scoped number
        project.setTaskCounter(project.getTaskCounter() + 1);
        projectRepository.save(project);

        Task task = new Task();
        task.setTaskNumber(project.getTaskCounter());
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
            task.setAssignee(assignee);
        }

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public TaskResponse getTaskByNumber(Long workspaceId, Long projectNumber, Long taskNumber) {
        Project project = findProject(workspaceId, projectNumber);
        Task task = taskRepository.findByProjectIdAndTaskNumber(project.getId(), taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: taskNumber=" + taskNumber + " in project " + projectNumber));
        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long workspaceId, Long projectNumber) {
        Project project = findProject(workspaceId, projectNumber);
        return taskRepository.findByProjectId(project.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> searchTasks(Long workspaceId, Long projectNumber,
            TaskSearchCriteria criteria, Pageable pageable) {
        Project project = findProject(workspaceId, projectNumber);
        Specification<Task> spec = TaskSpecification.getSpecification(project.getId(), criteria);
        return taskRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public TaskResponse updateTask(Long workspaceId, Long projectNumber, Long taskNumber, TaskRequest request) {
        Project project = findProject(workspaceId, projectNumber);
        Task task = taskRepository.findByProjectIdAndTaskNumber(project.getId(), taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: taskNumber=" + taskNumber + " in project " + projectNumber));

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
            task.setAssignee(assignee);
        } else {
            task.setAssignee(null); // Unassign if null
        }

        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long workspaceId, Long projectNumber, Long taskNumber, TaskStatus status) {
        Project project = findProject(workspaceId, projectNumber);
        Task task = taskRepository.findByProjectIdAndTaskNumber(project.getId(), taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: taskNumber=" + taskNumber + " in project " + projectNumber));
        task.setStatus(status);
        Task updatedTask = taskRepository.save(task);
        return mapToResponse(updatedTask);
    }

    @Transactional
    public void deleteTask(Long workspaceId, Long projectNumber, Long taskNumber) {
        Project project = findProject(workspaceId, projectNumber);
        Task task = taskRepository.findByProjectIdAndTaskNumber(project.getId(), taskNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task not found: taskNumber=" + taskNumber + " in project " + projectNumber));
        taskRepository.delete(task);
    }

    /**
     * Resolve a project from workspace-scoped identifiers.
     */
    private Project findProject(Long workspaceId, Long projectNumber) {
        return projectRepository.findByWorkspaceIdAndProjectNumber(workspaceId, projectNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: workspace=" + workspaceId + ", projectNumber=" + projectNumber));
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
                task.getProject().getProjectNumber(),
                task.getProject().getName());
    }
}
