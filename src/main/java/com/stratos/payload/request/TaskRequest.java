package com.stratos.payload.request;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {
    @NotBlank
    private String title;

    private String description;

    private TaskStatus status; // Optional, default TODO

    private TaskPriority priority; // Optional, default MEDIUM

    private LocalDate dueDate;

    private Long assigneeId; // Optional

    @NotNull
    private Long projectId;
}
