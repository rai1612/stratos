package com.stratos.payload.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
}
