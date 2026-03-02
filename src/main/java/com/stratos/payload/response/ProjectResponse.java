package com.stratos.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {
    private Long id;
    private String name;
    private String projectKey;
    private Long projectNumber;
    private Long workspaceId;
}
