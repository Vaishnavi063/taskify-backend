package com.taskify.backend.controllers.project;

import com.taskify.backend.dto.Document.*;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.ProjectDocumentService;
import com.taskify.backend.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/project/document")
@RequiredArgsConstructor
public class ProjectDocumentController {

    private final ProjectDocumentService projectDocumentService;
    
    @GetMapping("/test")
    public ApiResponse<String> test() {
        return ApiResponse.success("Controller is working!", "Test successful", 200);
    }
    
    @GetMapping("/getDocument")
    public ApiResponse<Map<String, Object>> getDocument(
        HttpServletRequest httpRequest,
        @Valid @ModelAttribute GetDocumentQueryDto query
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.getDocument(user, query);
        return ApiResponse.success(response, "Document fetched successfully", HttpStatus.OK.value());
    }
    
    @GetMapping
    public ApiResponse<Map<String, Object>> getDocuments(
        HttpServletRequest httpRequest,
        @Valid @ModelAttribute GetDocumentsQueryDto query
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.getDocuments(user, query);
        return ApiResponse.success(response, "Documents retrieved successfully", HttpStatus.OK.value());
    }
    
    @PostMapping
    public ApiResponse<Map<String, Object>> createDocument(
        HttpServletRequest httpRequest,
        @Valid @RequestBody CreateDocumentDto request
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.createDocument(user, request);
        return ApiResponse.success(response, "Document created successfully", HttpStatus.CREATED.value());
    }
    
    @PatchMapping
    public ApiResponse<Map<String, Object>> updateDocument(
        HttpServletRequest httpRequest,
        @Valid @RequestBody UpdateDocumentDto request
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.updateDocument(user, request);
        return ApiResponse.success(response, "Document updated successfully", HttpStatus.OK.value());
    }
    
    @DeleteMapping
    public ApiResponse<Map<String, Object>> deleteDocument(
        HttpServletRequest httpRequest,
        @Valid @RequestBody DeleteDocumentDto request
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.deleteDocument(user, request);
        return ApiResponse.success(response, "Document deleted successfully", HttpStatus.NO_CONTENT.value());
    }
    
    @PostMapping("/assign")
    public ApiResponse<Map<String, Object>> assignMember(
        HttpServletRequest httpRequest,
        @Valid @RequestBody AssignMemberDto request
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.assignMember(user, request);
        return ApiResponse.success(response, "Member assigned to document successfully", HttpStatus.OK.value());
    }
    
    @DeleteMapping("/assign")
    public ApiResponse<Map<String, Object>> removeMember(
        HttpServletRequest httpRequest,
        @Valid @RequestBody AssignMemberDto request
    ) {
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = projectDocumentService.removeMember(user, request);
        return ApiResponse.success(response, "Assigned member removed successfully", HttpStatus.OK.value());
    }
}