package com.taskify.backend.models.project;

import com.taskify.backend.constants.IssueEnums.IssuePriority;
import com.taskify.backend.constants.IssueEnums.IssueStatus;
import com.taskify.backend.models.auth.User;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "issues")
public class Issue {

    @Id
    private String id;

    private Member memberId;
    private Project projectId;
    private User userId;

    private String title;
    private String description;

    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @Builder.Default
    private IssuePriority priority = IssuePriority.LOW;

    @Builder.Default
    private List<Member> assignees = List.of();

    @Builder.Default
    private List<Comment> comments = List.of();

    @Builder.Default
    private List<String> labels = List.of();

    private LocalDateTime closedDate;

    private Member closedBy;

    @Builder.Default
    private Boolean isDeleted = false;
}
