package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetTeamsQueryValidator {
    @NotBlank(message = "Project ID is required")
    private String projectId;

    private String name = "";
    private Integer page = 1;
    private Integer limit = 10;
}
