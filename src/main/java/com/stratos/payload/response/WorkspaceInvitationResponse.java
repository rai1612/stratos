package com.stratos.payload.response;

import com.stratos.workspace.InvitationStatus;
import com.stratos.workspace.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceInvitationResponse {
    private UUID id;
    private UUID workspaceId;
    private String workspaceName;
    private String email;
    private String token;
    private WorkspaceRole role;
    private InvitationStatus status;
    private UUID invitedById;
    private Instant invitedAt;
    private Instant expiresAt;
}
