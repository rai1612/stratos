package com.stratos.payload.response;

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
public class WorkspaceMemberResponse {
    private UUID id;
    private UUID userId;
    private String username;
    private String email;
    private WorkspaceRole role;
    private Instant joinedAt;
}
