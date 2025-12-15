package com.taskify.backend.dto.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskify.backend.models.project.ProjectDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {
    
    @JsonProperty("_id")
    private String id;
    
    private String memberId;
    private String projectId;
    private String title;
    private String content;
    private String status;
    private String accessType;
    private String docType;
    private List<String> assignees;
    private List<String> teams;
    private List<String> comments;
    private Boolean isDeleted;
    private Integer leftMargin;
    private Integer rightMargin;
    private Instant createdAt;
    private Instant updatedAt;
    
    @JsonProperty("__v")
    private Integer version;
    
    public static DocumentResponseDto fromEntity(ProjectDocument doc) {
        return DocumentResponseDto.builder()
            .id(doc.getId())
            .memberId(doc.getMemberId())
            .projectId(doc.getProjectId())
            .title(doc.getTitle())
            .content(doc.getContent())
            .status(doc.getStatus().getValue())
            .accessType(doc.getAccessType().getValue())
            .docType(doc.getDocType().getValue())
            .assignees(doc.getAssignees())
            .teams(doc.getTeams())
            .comments(doc.getComments())
            .isDeleted(doc.getIsDeleted())
            .leftMargin(doc.getLeftMargin())
            .rightMargin(doc.getRightMargin())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .version(doc.get__v())
            .build();
    }
}