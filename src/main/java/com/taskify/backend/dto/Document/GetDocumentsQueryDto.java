package com.taskify.backend.dto.Document;

import com.taskify.backend.constants.DocumentEnums.DocStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetDocumentsQueryDto {
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
    
    private String title = "";
    
    private Boolean createdByMe = false;
    
    private Boolean assignedToMe = false;
    
    private DocStatus status;
    
    private Boolean sortByCreated = false;
    
    private Boolean isPublic = false;
    
    private Integer limit = 10;
    
    private Integer page = 1;
}