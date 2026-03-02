package com.stratos.task;

import com.stratos.model.BaseEntity;
import com.stratos.project.Project;
import com.stratos.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tasks", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = { "project_id", "task_number" })
})
public class Task extends BaseEntity {

    @Column(name = "task_number", nullable = false)
    private Long taskNumber; // Scoped within project: 1, 2, 3...

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    private Instant dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", referencedColumnName = "id")
    private User assignee;
}
