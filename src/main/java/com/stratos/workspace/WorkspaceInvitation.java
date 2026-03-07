package com.stratos.workspace;

import com.stratos.model.BaseEntity;
import com.stratos.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "workspace_invitations")
public class WorkspaceInvitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id", nullable = false)
    private User invitedBy;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public WorkspaceInvitation(Workspace workspace, String email, WorkspaceRole role, User invitedBy) {
        this.workspace = workspace;
        this.email = email;
        this.role = role;
        this.invitedBy = invitedBy;
        this.token = UUID.randomUUID().toString();
        this.status = InvitationStatus.PENDING;
        this.invitedAt = Instant.now();
        this.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
    }
}
