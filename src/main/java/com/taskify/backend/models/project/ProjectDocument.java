package com.taskify.backend.models.project;

import com.taskify.backend.constants.DocumentEnums.DocStatus;
import com.taskify.backend.constants.DocumentEnums.DocAccessType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class ProjectDocument {

    @Id
    private String id;

    private String memberId;  // ✨ Changed: Store ID only
    private String projectId;  // ✨ Changed: Store ID only

    private String title;
    private String content;

    @Builder.Default
    private DocStatus status = DocStatus.DRAFT;

    @Builder.Default
    private DocAccessType accessType = DocAccessType.PRIVATE;

    @Builder.Default
    private List<String> assignees = List.of();

    @Builder.Default
    private List<String> teams = List.of();

    @Builder.Default
    private List<String> comments = List.of();

    @Builder.Default
    private Boolean isDeleted = false;

    @Builder.Default
    private Integer leftMargin = 56;

    @Builder.Default
    private Integer rightMargin = 56;
}