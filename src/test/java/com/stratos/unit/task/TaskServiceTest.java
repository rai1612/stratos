package com.stratos.unit.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stratos.exception.ResourceNotFoundException;
import com.stratos.payload.request.TaskRequest;
import com.stratos.payload.response.TaskResponse;
import com.stratos.project.Project;
import com.stratos.project.ProjectRepository;
import com.stratos.project.ProjectService;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskRepository;
import com.stratos.task.TaskService;
import com.stratos.task.TaskStatus;
import com.stratos.task.Task;
import com.stratos.user.User;
import com.stratos.user.UserRepository;
import com.stratos.workspace.Workspace;
import com.stratos.workspace.WorkspaceMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

        @Mock
        private TaskRepository taskRepository;

        @Mock
        private ProjectService projectService;

        @Mock
        private ProjectRepository projectRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private WorkspaceMemberRepository workspaceMemberRepository;

        @InjectMocks
        private TaskService taskService;

        private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
        private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000100");
        private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000001000");
        private static final UUID ASSIGNEE_ID = UUID.fromString("00000000-0000-0000-0000-000000002000");
        private static final String PROJECT_KEY = "TEST";

        private Project createTestProject() {
                Workspace workspace = new Workspace();
                workspace.setId(WORKSPACE_ID);
                workspace.setName("Test Workspace");
                Project project = new Project();
                project.setId(PROJECT_ID);
                project.setName("Test Project");
                project.setProjectKey(PROJECT_KEY);
                project.setTaskCounter(0L);
                project.setWorkspace(workspace);
                return project;
        }

        private User createTestAssignee() {
                User assignee = new User("assignee", "assignee@stratos.com", "hashed");
                assignee.setId(ASSIGNEE_ID);
                return assignee;
        }

        private Task createTestTask(Project project, User assignee) {
                Task task = new Task();
                task.setId(TASK_ID);
                task.setTaskNumber(1L);
                task.setTitle("Test Task");
                task.setDescription("Task description");
                task.setStatus(TaskStatus.TODO);
                task.setPriority(TaskPriority.MEDIUM);
                task.setProject(project);
                task.setAssignee(assignee);
                return task;
        }

        @Nested
        class CreateTask {

                @Test
                void shouldCreateTaskWithSequentialNumber() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
                        when(projectRepository.save(any(Project.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(WORKSPACE_ID, ASSIGNEE_ID))
                                        .thenReturn(true);
                        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                                Task t = invocation.getArgument(0);
                                t.setId(TASK_ID);
                                return t;
                        });

                        TaskRequest request = new TaskRequest();
                        request.setTitle("New Task");
                        request.setStatus(TaskStatus.TODO);
                        request.setPriority(TaskPriority.HIGH);
                        request.setAssigneeId(ASSIGNEE_ID);

                        TaskResponse response = taskService.createTask(WORKSPACE_ID, PROJECT_KEY, request);

                        assertThat(response.getTitle()).isEqualTo("New Task");
                        assertThat(response.getTaskNumber()).isEqualTo(1L);
                        assertThat(response.getTaskKey()).isEqualTo("TEST-1");
                        assertThat(response.getAssigneeId()).isEqualTo(ASSIGNEE_ID);
                        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
                }

                @Test
                void shouldCreateTaskWithoutAssignee() {
                        Project project = createTestProject();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
                        when(projectRepository.save(any(Project.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                                Task t = invocation.getArgument(0);
                                t.setId(TASK_ID);
                                return t;
                        });

                        TaskRequest request = new TaskRequest();
                        request.setTitle("Unassigned Task");
                        request.setStatus(TaskStatus.TODO);

                        TaskResponse response = taskService.createTask(WORKSPACE_ID, PROJECT_KEY, request);

                        assertThat(response.getAssigneeId()).isNull();
                        assertThat(response.getTaskNumber()).isEqualTo(1L);
                }

                @Test
                void shouldRejectDoneStatusOnCreation() {
                        Project project = createTestProject();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);

                        TaskRequest request = new TaskRequest();
                        request.setTitle("Done Task");
                        request.setStatus(TaskStatus.DONE);

                        assertThatThrownBy(() -> taskService.createTask(WORKSPACE_ID, PROJECT_KEY, request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("DONE");
                }

                @Test
                void shouldDefaultStatusToTodoAndPriorityToMedium() {
                        Project project = createTestProject();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(projectRepository.findByIdForUpdate(PROJECT_ID)).thenReturn(Optional.of(project));
                        when(projectRepository.save(any(Project.class)))
                                        .thenAnswer(invocation -> invocation.getArgument(0));
                        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
                                Task t = invocation.getArgument(0);
                                t.setId(TASK_ID);
                                return t;
                        });

                        TaskRequest request = new TaskRequest();
                        request.setTitle("Default Task");

                        TaskResponse response = taskService.createTask(WORKSPACE_ID, PROJECT_KEY, request);

                        assertThat(response.getStatus()).isEqualTo(TaskStatus.TODO);
                        assertThat(response.getPriority()).isEqualTo(TaskPriority.MEDIUM);
                }
        }

        @Nested
        class GetTask {

                @Test
                void shouldGetTaskByProjectKeyAndNumber() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        Task task = createTestTask(project, assignee);
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 1L)).thenReturn(Optional.of(task));

                        TaskResponse response = taskService.getTask(WORKSPACE_ID, PROJECT_KEY, 1L);

                        assertThat(response.getId()).isEqualTo(TASK_ID);
                        assertThat(response.getTitle()).isEqualTo("Test Task");
                        assertThat(response.getTaskKey()).isEqualTo("TEST-1");
                }

                @Test
                void shouldThrowWhenTaskNotFound() {
                        Project project = createTestProject();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 99L)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> taskService.getTask(WORKSPACE_ID, PROJECT_KEY, 99L))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Task not found");
                }
        }

        @Nested
        class GetTasksByProject {

                @Test
                void shouldReturnTasksForProject() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        Task task = createTestTask(project, assignee);
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectId(PROJECT_ID)).thenReturn(List.of(task));

                        List<TaskResponse> responses = taskService.getTasksByProject(WORKSPACE_ID, PROJECT_KEY);

                        assertThat(responses).hasSize(1);
                        assertThat(responses.get(0).getTitle()).isEqualTo("Test Task");
                }
        }

        @Nested
        class UpdateTask {

                @Test
                void shouldUpdateTaskSuccessfully() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        Task task = createTestTask(project, assignee);
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 1L)).thenReturn(Optional.of(task));
                        when(userRepository.findById(ASSIGNEE_ID)).thenReturn(Optional.of(assignee));
                        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(WORKSPACE_ID, ASSIGNEE_ID))
                                        .thenReturn(true);
                        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

                        TaskRequest request = new TaskRequest();
                        request.setTitle("Updated Task");
                        request.setStatus(TaskStatus.IN_PROGRESS);
                        request.setPriority(TaskPriority.HIGH);
                        request.setAssigneeId(ASSIGNEE_ID);

                        TaskResponse response = taskService.updateTask(WORKSPACE_ID, PROJECT_KEY, 1L, request);

                        assertThat(response.getTitle()).isEqualTo("Updated Task");
                        assertThat(response.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
                }
        }

        @Nested
        class DeleteTask {

                @Test
                void shouldDeleteTaskSuccessfully() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        Task task = createTestTask(project, assignee);
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 1L)).thenReturn(Optional.of(task));

                        taskService.deleteTask(WORKSPACE_ID, PROJECT_KEY, 1L);

                        verify(taskRepository).delete(task);
                }

                @Test
                void shouldThrowWhenDeletingNonExistentTask() {
                        Project project = createTestProject();
                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 99L)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> taskService.deleteTask(WORKSPACE_ID, PROJECT_KEY, 99L))
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Task not found");
                }
        }

        @Nested
        class MapToResponse {

                @Test
                void shouldMapTaskWithAssigneeToResponse() {
                        Project project = createTestProject();
                        User assignee = createTestAssignee();
                        Task task = createTestTask(project, assignee);

                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 1L)).thenReturn(Optional.of(task));
                        TaskResponse response = taskService.getTask(WORKSPACE_ID, PROJECT_KEY, 1L);

                        assertThat(response.getAssigneeId()).isEqualTo(ASSIGNEE_ID);
                        assertThat(response.getAssigneeUsername()).isEqualTo("assignee");
                        assertThat(response.getProjectId()).isEqualTo(PROJECT_ID);
                        assertThat(response.getProjectName()).isEqualTo("Test Project");
                        assertThat(response.getTaskKey()).isEqualTo("TEST-1");
                }

                @Test
                void shouldMapTaskWithoutAssigneeToResponse() {
                        Project project = createTestProject();
                        Task task = createTestTask(project, null);

                        when(projectService.findByWorkspaceAndKey(WORKSPACE_ID, PROJECT_KEY)).thenReturn(project);
                        when(taskRepository.findByProjectIdAndTaskNumber(PROJECT_ID, 1L)).thenReturn(Optional.of(task));
                        TaskResponse response = taskService.getTask(WORKSPACE_ID, PROJECT_KEY, 1L);

                        assertThat(response.getAssigneeId()).isNull();
                        assertThat(response.getAssigneeUsername()).isNull();
                }
        }
}
