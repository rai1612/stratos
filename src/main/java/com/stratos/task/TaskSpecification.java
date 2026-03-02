package com.stratos.task;

import com.stratos.payload.request.TaskSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class TaskSpecification {

    public static Specification<Task> getSpecification(Long projectId, TaskSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always scope to the resolved project
            predicates.add(criteriaBuilder.equal(root.get("project").get("id"), projectId));

            if (criteria.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), criteria.getStatus()));
            }

            if (criteria.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), criteria.getPriority()));
            }

            if (criteria.getAssigneeId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), criteria.getAssigneeId()));
            }

            if (criteria.getDueDateStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("dueDate"),
                        criteria.getDueDateStart().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }

            if (criteria.getDueDateEnd() != null) {
                // End of day
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("dueDate"),
                        criteria.getDueDateEnd().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
