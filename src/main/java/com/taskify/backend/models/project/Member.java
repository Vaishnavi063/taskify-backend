package com.taskify.backend.models.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.models.auth.User;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "members")
public class Member {

    @Id
    private String id;

    @DBRef
    private User user;

    @DBRef
    private Project project;

    private String email;

    private String role = MemberRole.MEMBER.name();

    private String invitationStatus = InvitationStatus.PENDING.name();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
