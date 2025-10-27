package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProjectValidator {
    @NotEmpty(message = "Project name is required")
    @Size(min = 3, message = "Project name must be at least 3 characters long")
    private String name;

    @NotEmpty(message = "Description is required")
    private String description;

    private List<String> tags;
}