package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class RemoveMemberValidator {

    @NotEmpty(message = "Project ID is required")
    private String projectId;

    @NotEmpty(message = "Member ID is required")
    private String memberId;
}
