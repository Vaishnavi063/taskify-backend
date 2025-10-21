package com.taskify.backend.validators.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenValidator {
    @NotBlank(message = "Token is required")
    private String token;
}

