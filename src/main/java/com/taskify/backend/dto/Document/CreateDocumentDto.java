package com.taskify.backend.dto.Document;

import com.taskify.backend.constants.DocumentEnums.DocAccessType;
import com.taskify.backend.constants.DocumentEnums.DocStatus;
import com.taskify.backend.constants.DocumentEnums.DocType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentDto {
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String content;
    
    private DocStatus status = DocStatus.DRAFT;
    
    private DocAccessType accessType = DocAccessType.PRIVATE;
    
    private DocType docType = DocType.DOCUMENT;
    
    private Integer leftMargin = 56;
    
    private Integer rightMargin = 56;
}