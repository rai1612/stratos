package com.stratos.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Size(min = 3, max = 10)
    private String projectKey;
}
