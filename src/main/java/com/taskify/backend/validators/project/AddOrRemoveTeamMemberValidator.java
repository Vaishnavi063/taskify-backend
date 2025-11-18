package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddOrRemoveTeamMemberValidator {
    @NotBlank(message = "Team ID is required")
    private String teamId;

    @NotBlank(message = "Project ID is required")
    private String projectId;

    @NotBlank(message = "Member ID is required")
    private String memberId;

    private Boolean isRemove = false;
}
