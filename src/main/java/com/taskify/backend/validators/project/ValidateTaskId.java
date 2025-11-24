package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ValidateTaskId {
    @NotEmpty(message = "Task ID required")
    private String taskId;

    private String projectId;
}
