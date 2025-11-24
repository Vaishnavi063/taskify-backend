package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabelValidator {
    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;
    private String color;
}
