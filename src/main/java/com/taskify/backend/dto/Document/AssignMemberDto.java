package com.taskify.backend.dto.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignMemberDto {
    
    @NotBlank(message = "Document ID is required")
    private String docId;
    
    @NotBlank(message = "Member ID is required")
    private String memberId;
    
    @NotBlank(message = "Project ID is required")
    private String projectId;
}