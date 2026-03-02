package com.stratos.payload.request;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskSearchCriteria {
    private TaskStatus status;
    private TaskPriority priority;
    private Long assigneeId;
    private LocalDate dueDateStart;
    private LocalDate dueDateEnd;
}
