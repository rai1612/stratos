package com.stratos.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceMemberUpdateRoleRequest {
    @NotBlank
    private String role;
}
