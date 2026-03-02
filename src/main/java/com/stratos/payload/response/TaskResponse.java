package com.stratos.payload.response;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {
    private Long id;
    private Long taskNumber;
    private String taskKey; // e.g. "STRAT-3"
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Long assigneeId;
    private String assigneeUsername;
    private Long projectNumber;
    private String projectName;
}
