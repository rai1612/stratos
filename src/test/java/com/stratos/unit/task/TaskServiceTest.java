package com.stratos.unit.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.request.TaskSearchCriteria;
import com.stratos.payload.response.TaskResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.task.Task;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskRepository;
import com.stratos.task.TaskService;
import com.stratos.task.TaskStatus;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import com.stratos.workspace.Workspace;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService taskService;

    private Project project;
    private Workspace workspace;
    private Task task;
    private User assignee;

    @BeforeEach
    void setUp() {
        workspace = new Workspace();
        workspace.setId(1L);
        workspace.setName("Test WS");
        workspace.setProjectCounter(1L);

        project = new Project();
        project.setId(10L);
        project.setName("Test Project");
        project.setProjectKey("TST");
        project.setProjectNumber(1L);
        project.setTaskCounter(0L);
        project.setWorkspace(workspace);

        assignee = new User("testuser", "test@stratos.com", "hashed");
        assignee.setId(100L);

        task = new Task();
        task.setId(1000L);
        task.setTaskNumber(1L);
        task.setTitle("Test Task");
        task.setDescription("A description");
        task.setStatus(TaskStatus.TODO);
        task.setPriority(TaskPriority.MEDIUM);
        task.setProject(project);
    }

    // ========================================================================
    // createTask
    // ========================================================================

    @Nested
    class CreateTask {

        @Test
        void shouldCreateTaskWithDefaults() {
            TaskRequest request = new TaskRequest();
            request.setTitle("New Task");

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task saved = invocation.getArgument(0);
                saved.setId(1000L);
                return saved;
            });

            TaskResponse response = taskService.createTask(1L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("New Task");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
            assertThat(response.getTaskNumber()).isEqualTo(1L);
            assertThat(response.getTaskKey()).isEqualTo("TST-1");
        }

        @Test
        void shouldCreateTaskWithExplicitFields() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Explicit Task");
            request.setDescription("Details");
            request.setStatus(TaskStatus.IN_PROGRESS);
            request.setPriority(TaskPriority.HIGH);
            request.setDueDate(LocalDate.of(2026, 6, 15));

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task saved = invocation.getArgument(0);
                saved.setId(1000L);
                return saved;
            });

            TaskResponse response = taskService.createTask(1L, 1L, request);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(response.getPriority()).isEqualTo(TaskPriority.HIGH);
            assertThat(response.getDescription()).isEqualTo("Details");
            assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        }

        @Test
        void shouldCreateTaskWithAssignee() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Assigned Task");
            request.setAssigneeId(100L);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(userRepository.findById(100L)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task saved = invocation.getArgument(0);
                saved.setId(1000L);
                return saved;
            });

            TaskResponse response = taskService.createTask(1L, 1L, request);

            assertThat(response.getAssigneeId()).isEqualTo(100L);
            assertThat(response.getAssigneeUsername()).isEqualTo("testuser");
        }

        @Test
        void shouldIncrementTaskCounterAndAssignNumber() {
            assertThat(project.getTaskCounter()).isEqualTo(0L);

            TaskRequest request = new TaskRequest();
            request.setTitle("Counter Task");

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                Task saved = invocation.getArgument(0);
                saved.setId(1000L);
                return saved;
            });

            taskService.createTask(1L, 1L, request);

            // Verify counter was incremented
            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(projectCaptor.capture());
            assertThat(projectCaptor.getValue().getTaskCounter()).isEqualTo(1L);

            // Verify task number was assigned from counter
            ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
            verify(taskRepository).save(taskCaptor.capture());
            assertThat(taskCaptor.getValue().getTaskNumber()).isEqualTo(1L);
        }

        @Test
        void shouldThrowWhenStatusIsDone() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Done Task");
            request.setStatus(TaskStatus.DONE);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));

            assertThatThrownBy(() -> taskService.createTask(1L, 1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Task status cannot be DONE on creation");

            verify(taskRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            TaskRequest request = new TaskRequest();
            request.setTitle("No Project");

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowWhenAssigneeNotFound() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Bad Assignee");
            request.setAssigneeId(999L);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.findByIdForUpdate(10L))
                    .thenReturn(Optional.of(project));
            when(projectRepository.save(project)).thenReturn(project);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.createTask(1L, 1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    // ========================================================================
    // getTaskByNumber
    // ========================================================================

    @Nested
    class GetTaskByNumber {

        @Test
        void shouldReturnMappedResponse() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskByNumber(1L, 1L, 1L);

            assertThat(response.getTitle()).isEqualTo("Test Task");
            assertThat(response.getTaskKey()).isEqualTo("TST-1");
            assertThat(response.getProjectNumber()).isEqualTo(1L);
            assertThat(response.getProjectName()).isEqualTo("Test Project");
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskByNumber(1L, 999L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskByNumber(1L, 1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // getTasksByProject
    // ========================================================================

    @Nested
    class GetTasksByProject {

        @Test
        void shouldReturnMappedList() {
            Task task2 = new Task();
            task2.setId(1001L);
            task2.setTaskNumber(2L);
            task2.setTitle("Task 2");
            task2.setStatus(TaskStatus.IN_PROGRESS);
            task2.setPriority(TaskPriority.HIGH);
            task2.setProject(project);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectId(10L))
                    .thenReturn(List.of(task, task2));

            List<TaskResponse> responses = taskService.getTasksByProject(1L, 1L);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getTitle()).isEqualTo("Test Task");
            assertThat(responses.get(1).getTitle()).isEqualTo("Task 2");
        }

        @Test
        void shouldReturnEmptyList() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectId(10L))
                    .thenReturn(Collections.emptyList());

            List<TaskResponse> responses = taskService.getTasksByProject(1L, 1L);

            assertThat(responses).isEmpty();
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTasksByProject(1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // searchTasks
    // ========================================================================

    @Nested
    class SearchTasks {

        @Test
        @SuppressWarnings("unchecked")
        void shouldDelegateToSpecification() {
            Pageable pageable = PageRequest.of(0, 10);
            TaskSearchCriteria criteria = new TaskSearchCriteria();
            Page<Task> taskPage = new PageImpl<>(List.of(task));

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(taskPage);

            Page<TaskResponse> result = taskService.searchTasks(1L, 1L, criteria, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Test Task");
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.searchTasks(1L, 999L,
                    new TaskSearchCriteria(), PageRequest.of(0, 10)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // updateTask
    // ========================================================================

    @Nested
    class UpdateTask {

        @Test
        void shouldUpdateAllFields() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Updated Title");
            request.setDescription("Updated Desc");
            request.setStatus(TaskStatus.DONE);
            request.setPriority(TaskPriority.LOW);
            request.setDueDate(LocalDate.of(2026, 12, 25));
            request.setAssigneeId(100L);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));
            when(userRepository.findById(100L)).thenReturn(Optional.of(assignee));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            TaskResponse response = taskService.updateTask(1L, 1L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getDescription()).isEqualTo("Updated Desc");
            assertThat(response.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(response.getPriority()).isEqualTo(TaskPriority.LOW);
            assertThat(response.getAssigneeId()).isEqualTo(100L);
        }

        @Test
        void shouldPreserveStatusAndPriorityWhenNull() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Partial Update");
            // status and priority are null in request

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            TaskResponse response = taskService.updateTask(1L, 1L, 1L, request);

            // Original values preserved
            assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        }

        @Test
        void shouldUnassignWhenAssigneeIdIsNull() {
            task.setAssignee(assignee); // Initially assigned

            TaskRequest request = new TaskRequest();
            request.setTitle("Unassign Task");
            // assigneeId is null — should unassign

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            TaskResponse response = taskService.updateTask(1L, 1L, 1L, request);

            assertThat(response.getAssigneeId()).isNull();
            assertThat(response.getAssigneeUsername()).isNull();
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Not Found");

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(1L, 1L, 999L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void shouldThrowWhenAssigneeNotFound() {
            TaskRequest request = new TaskRequest();
            request.setTitle("Bad Assignee Update");
            request.setAssigneeId(999L);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(1L, 1L, 1L, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // updateTaskStatus
    // ========================================================================

    @Nested
    class UpdateTaskStatus {

        @Test
        void shouldUpdateStatus() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));
            when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

            TaskResponse response = taskService.updateTaskStatus(1L, 1L, 1L, TaskStatus.IN_PROGRESS);

            assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTaskStatus(1L, 1L, 999L, TaskStatus.DONE))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // deleteTask
    // ========================================================================

    @Nested
    class DeleteTask {

        @Test
        void shouldDeleteTask() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            taskService.deleteTask(1L, 1L, 1L);

            verify(taskRepository).delete(task);
        }

        @Test
        void shouldThrowWhenTaskNotFound() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.deleteTask(1L, 1L, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ========================================================================
    // mapToResponse — tested via public methods
    // ========================================================================

    @Nested
    class ResponseMapping {

        @Test
        void shouldComputeTaskKey() {
            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskByNumber(1L, 1L, 1L);

            assertThat(response.getTaskKey()).isEqualTo("TST-1");
        }

        @Test
        void shouldHandleNullAssignee() {
            task.setAssignee(null);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskByNumber(1L, 1L, 1L);

            assertThat(response.getAssigneeId()).isNull();
            assertThat(response.getAssigneeUsername()).isNull();
        }

        @Test
        void shouldHandleNullDueDate() {
            task.setDueDate(null);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskByNumber(1L, 1L, 1L);

            assertThat(response.getDueDate()).isNull();
        }

        @Test
        void shouldMapDueDateCorrectly() {
            Instant dueInstant = LocalDate.of(2026, 7, 4)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant();
            task.setDueDate(dueInstant);

            when(projectRepository.findByWorkspaceIdAndProjectNumber(1L, 1L))
                    .thenReturn(Optional.of(project));
            when(taskRepository.findByProjectIdAndTaskNumber(10L, 1L))
                    .thenReturn(Optional.of(task));

            TaskResponse response = taskService.getTaskByNumber(1L, 1L, 1L);

            assertThat(response.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        }
    }
}
