package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ValidateProjectIdQuery {
    private String projectId;
}
