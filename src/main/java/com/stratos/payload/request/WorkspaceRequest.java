package com.stratos.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceRequest {
    @NotBlank
    private String name;

    private String description;
}
