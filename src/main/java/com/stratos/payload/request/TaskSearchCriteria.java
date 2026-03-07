package com.stratos.payload.request;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskSearchCriteria {
    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;
    private LocalDate dueDateStart;
    private LocalDate dueDateEnd;
}
