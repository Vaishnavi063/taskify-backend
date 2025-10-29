package com.taskify.backend.validators.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class changeInvitationStatusValidator {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be a valid email")
    private String email;

    @NotNull(message = "Invitation status required")
    private InvitationStatus invitationStatus;

    @NotBlank(message = "Invitation token is required")
    private String invitationToken;
}
