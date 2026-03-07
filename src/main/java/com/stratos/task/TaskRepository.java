package com.stratos.task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository
        extends JpaRepository<Task, UUID>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Task> {
    List<Task> findByProjectId(UUID projectId);

    List<Task> findByAssigneeId(UUID assigneeId);

    Optional<Task> findByProjectIdAndTaskNumber(UUID projectId, Long taskNumber);

    @Modifying
    @Query("UPDATE Task t SET t.assignee = null WHERE t.assignee.id = :assigneeId AND t.project.id IN (SELECT p.id FROM Project p WHERE p.workspace.id = :workspaceId)")
    void unassignTasksByWorkspaceAndAssignee(@Param("workspaceId") UUID workspaceId,
            @Param("assigneeId") UUID assigneeId);
}
