package com.stratos.task;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository
        extends JpaRepository<Task, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Task> {
    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssigneeId(Long assigneeId);
}
