package com.stratos.project;

import com.stratos.model.BaseEntity;
import com.stratos.workspace.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "projects", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = { "workspace_id", "project_key" })
})
public class Project extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "project_key", nullable = false, length = 10)
    private String projectKey; // e.g. "STRAT" for Stratos Project

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", referencedColumnName = "id", nullable = false)
    private Workspace workspace;
}
