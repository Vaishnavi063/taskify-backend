package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ProjectIdQueryValidator {
    @NotEmpty(message = "ProjectId is required")
    private String projectId;
}