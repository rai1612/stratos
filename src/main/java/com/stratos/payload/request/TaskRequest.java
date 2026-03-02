package com.stratos.payload.request;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {
    @NotBlank(groups = OnCreate.class)
    @Size(max = 100, groups = { OnCreate.class, OnUpdate.class })
    private String title;

    @Size(max = 500, groups = { OnCreate.class, OnUpdate.class })
    private String description;

    private TaskStatus status; // Optional, default TODO

    private TaskPriority priority; // Optional, default MEDIUM

    @FutureOrPresent(message = "Due date must be today or in the future", groups = { OnCreate.class, OnUpdate.class })
    private LocalDate dueDate;

    private Long assigneeId; // Optional
}
