package com.taskify.backend.validators.project;


import com.taskify.backend.constants.TaskEnums.TaskStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
public class ChangeStatusValidator {
    @NotEmpty(message = "Project ID is required")
    private String taskId;

    @NotNull(message = "Task status required")
    private TaskStatus status;
}
