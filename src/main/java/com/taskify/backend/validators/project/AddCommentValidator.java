package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentValidator {
    @NotBlank(message = "Task ID is required")
    private String taskId;

    @NotBlank(message = "Content is required")
    private String content;
}
