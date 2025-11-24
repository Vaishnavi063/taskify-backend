package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class GetTaskQueryValidator {

    @NotEmpty(message = "Project ID is required")
    private String projectId;

    @NotEmpty(message = "Task Id is required")
    private String taskId;
}
