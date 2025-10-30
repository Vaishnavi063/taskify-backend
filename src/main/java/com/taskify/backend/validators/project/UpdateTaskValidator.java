package com.taskify.backend.validators.project;

import com.taskify.backend.constants.TaskEnums;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UpdateTaskValidator {

    @NotEmpty(message = "Project ID is required")
    private String projectId;

    @NotEmpty(message = "Task Id is required")
    private String taskId;

    private String title;

    private String description;

    private TaskEnums.TaskPriority priority;

    private TaskEnums.TaskStatus status;

    private String taskType;

    private LocalDate dueDate;

    private LocalDate completedDate;

    private List<String> subTasks = new ArrayList<>();
}