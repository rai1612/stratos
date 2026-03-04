package com.stratos.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.stratos.validation.ValidationGroups;

@Getter
@Setter
public class WorkspaceRequest {
    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 3, max = 50, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String name;

    @Size(max = 255, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String description;
}
