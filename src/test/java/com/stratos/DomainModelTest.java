package com.stratos;

import static org.assertj.core.api.Assertions.assertThat;

import com.stratos.project.Project;
import com.stratos.task.Task;
import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import com.stratos.user.User;
import com.stratos.workspace.Workspace;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DomainModelTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistAndRetrieveProjectHierarchy() {
        // 1. Create and Save User
        User user = new User();
        user.setUsername("akr");
        user.setEmail("akr@stratos.com");
        user.setPassword("hashed_secret");
        user = entityManager.persistFlushFind(user);

        // 2. Create and Save Workspace
        Workspace workspace = new Workspace();
        workspace.setName("Engineering");
        workspace.setOwner(user);
        workspace = entityManager.persistFlushFind(workspace);

        // 3. Create and Save Project
        Project project = new Project();
        project.setName("Stratos v1");
        project.setProjectKey("STRA");
        project.setWorkspace(workspace);
        project = entityManager.persistFlushFind(project);

        // 4. Create and Save Task
        Task task = new Task();
        task.setTitle("Design Database");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.HIGH);
        task.setProject(project);
        task.setAssignee(user);
        task = entityManager.persistFlushFind(task);

        // 5. Verification
        assertThat(task.getId()).isNotNull();
        assertThat(task.getProject().getName()).isEqualTo("Stratos v1");
        assertThat(task.getProject().getWorkspace().getName()).isEqualTo("Engineering");
        assertThat(task.getAssignee().getUsername()).isEqualTo("akr");
    }
}
