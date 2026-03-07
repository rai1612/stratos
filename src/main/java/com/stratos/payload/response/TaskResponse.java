package com.stratos.payload.response;

import com.stratos.task.TaskPriority;
import com.stratos.task.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {
    private UUID id;
    private Long taskNumber;
    private String taskKey;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private UUID assigneeId;
    private String assigneeUsername;
    private UUID projectId;
    private String projectName;
}
