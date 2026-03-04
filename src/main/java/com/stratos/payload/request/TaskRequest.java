package com.stratos.payload.request;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import com.stratos.validation.ValidationGroups;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskRequest {
    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(max = 100, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String title;

    @Size(max = 500, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String description;

    private TaskStatus status; // Optional, default TODO

    private TaskPriority priority; // Optional, default MEDIUM

    @FutureOrPresent(message = "Due date must be today or in the future", groups = { ValidationGroups.Create.class,
            ValidationGroups.Update.class })
    private LocalDate dueDate;

    private Long assigneeId; // Optional
}
