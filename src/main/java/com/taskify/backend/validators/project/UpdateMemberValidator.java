package com.taskify.backend.validators.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMemberValidator {

    @NotEmpty(message = "Project ID is required")
    private String projectId;

    @NotEmpty(message = "Member ID is required")
    private String memberId;

    @NotNull(message = "Role required")
    private MemberRole role;
}