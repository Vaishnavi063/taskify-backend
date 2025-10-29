package com.taskify.backend.validators.project;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class inviteMemberValidator {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be a valid email")
    private String email;

    @NotBlank(message = "Project ID is required")
    private String projectId;
}
