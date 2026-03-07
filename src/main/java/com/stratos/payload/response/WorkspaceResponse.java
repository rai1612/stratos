package com.stratos.payload.response;

import com.stratos.workspace.WorkspaceRole;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceResponse {
    private UUID id;
    private String name;
    private String description;
    private WorkspaceRole role;
}
