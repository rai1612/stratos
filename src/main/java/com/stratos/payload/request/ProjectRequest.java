package com.stratos.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import com.stratos.validation.ValidationGroups;

@Getter
@Setter
public class ProjectRequest {
    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 1, max = 50, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String name;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 3, max = 10, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String projectKey;
}
