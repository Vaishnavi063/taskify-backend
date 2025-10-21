package com.taskify.backend.validators.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FullNameValidator {
    @NotEmpty(message = "Full name is required")
    @Size(min = 3, message = "Full name must be at least 3 characters long")
    private String fullName;
}