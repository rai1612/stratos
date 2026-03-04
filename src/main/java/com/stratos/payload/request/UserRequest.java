package com.stratos.payload.request;

import com.stratos.validation.ValidationGroups;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 3, max = 20, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String username;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(max = 50, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    @Email(groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String email;

    @NotBlank(groups = ValidationGroups.Create.class)
    @Size(min = 6, max = 40, groups = { ValidationGroups.Create.class, ValidationGroups.Update.class })
    private String password;

    private Set<String> role;
}
