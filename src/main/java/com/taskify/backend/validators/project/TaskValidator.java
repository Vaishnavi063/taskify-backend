package com.taskify.backend.validators.project;

import com.taskify.backend.constants.TaskEnums.TaskStatus;
import com.taskify.backend.constants.TaskEnums.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskValidator {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "Priority is required")
    private TaskPriority priority;

    @NotNull(message = "Status is required")
    private TaskStatus status;

    @NotBlank(message = "Task type is required")
    private String taskType;

    @NotNull(message = "Due date is required")
    @FutureOrPresent(message = "Due date must be today or later")
    private LocalDate dueDate;

    @NotBlank(message = "Project ID is required")
    private String projectId;
}
