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
@Getter
@Setter
public class Member {

    @Id
    private String id;

    private String userId;

    private String projectId;

    private String email;

    private MemberRole role = MemberRole.MEMBER;

    private InvitationStatus invitationStatus = InvitationStatus.PENDING;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
