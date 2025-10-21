package com.taskify.backend.validators.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;


@Data
public class UserEmailValidator {
    @NotEmpty(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;
}
