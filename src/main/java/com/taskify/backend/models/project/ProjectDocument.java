package com.taskify.backend.models.project;

import com.taskify.backend.constants.DocumentEnums.DocStatus;
import com.taskify.backend.constants.DocumentEnums.DocAccessType;
import com.taskify.backend.constants.DocumentEnums.DocType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
public class ProjectDocument {

    @Id
    private String id;

    private String memberId;
    private String projectId;

    private String title;
    private String content;

    @Builder.Default
    private DocStatus status = DocStatus.DRAFT;

    @Builder.Default
    private DocAccessType accessType = DocAccessType.PRIVATE;
    
    @Builder.Default
    private DocType docType = DocType.DOCUMENT;

    @Builder.Default
    private List<String> assignees = new ArrayList<>();

    @Builder.Default
    private List<String> teams = new ArrayList<>();

    @Builder.Default
    private List<String> comments = new ArrayList<>();

    @Builder.Default
    private Boolean isDeleted = false;

    @Builder.Default
    private Integer leftMargin = 56;

    @Builder.Default
    private Integer rightMargin = 56;
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Builder.Default
    private Integer __v = 0;
}