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
public class UpdateDocumentDto {
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    @NotBlank(message = "Document ID is required")
    private String docId;
    
    private String title;
    
    private String content;
    
    private DocStatus status;
    
    private DocAccessType accessType;
    
    private DocType docType;
    
    private Integer leftMargin;
    
    private Integer rightMargin;
}