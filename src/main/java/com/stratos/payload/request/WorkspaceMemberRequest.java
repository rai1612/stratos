package com.stratos.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkspaceMemberRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String role;
}
