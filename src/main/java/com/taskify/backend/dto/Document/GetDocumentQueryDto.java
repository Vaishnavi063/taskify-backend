package com.taskify.backend.dto.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDocumentQueryDto {
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    @NotBlank(message = "Document ID is required")
    private String docId;
}