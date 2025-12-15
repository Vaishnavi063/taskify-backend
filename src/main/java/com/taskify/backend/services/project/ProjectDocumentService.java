package com.taskify.backend.services.project;

import com.taskify.backend.constants.CommentEnums.CommentType;
import com.taskify.backend.constants.DocumentEnums.DocAccessType;
import com.taskify.backend.constants.DocumentEnums.DocStatus;
import com.taskify.backend.constants.DocumentEnums.DocType;
import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.dto.Document.*;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Comment;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.models.project.ProjectDocument;
import com.taskify.backend.repository.project.*;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.utils.ApiException;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectDocumentService {

    private final ProjectDocumentRepository projectDocumentRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    
    @Value("${frontend.url}")
    private String frontendUrl;
    
    public Map<String, Object> getDocument(User user, GetDocumentQueryDto query) {
        String userId = user.getId();
        String projectId = query.getProjectId();
        String docId = query.getDocId();
        
        log.info("Getting document - userId: {}, projectId: {}, docId: {}", userId, projectId, docId);
        
        // Validate docId
        if (docId == null || docId.isEmpty() || docId.equals("undefined")) {
            throw new ApiException("Invalid document ID", 400);
        }
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ApiException("Project not found", 404));
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        Map<String, Object> doc = projectDocumentRepository.getFullDocumentById(docId, member.getId())
            .orElseThrow(() -> new ApiException("Document not found", 404));
        
        // Check access permissions
        String accessType = (String) doc.get("accessType");
        String docMemberId = (String) doc.get("memberId");
        
        if ("Private".equals(accessType) && !docMemberId.equals(member.getId())) {
            throw new ApiException("You do not have permission to get this document", 403);
        }
        
        return doc; // Return document directly, not wrapped in "doc"
    }
    
    public Map<String, Object> getDocuments(User user, GetDocumentsQueryDto query) {
        String userId = user.getId();
        String projectId = query.getProjectId();
        
        log.info("Getting documents - userId: {}, projectId: {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ApiException("Project not found", 404));
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        // Use custom repository method for complex aggregation with pagination
        Map<String, Object> paginatedDocs = projectDocumentRepository.getDocuments(projectId, member.getId(), query);
        
        return paginatedDocs;
    }
    
    public Map<String, Object> createDocument(User user, CreateDocumentDto request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        
        log.info("Creating document - userId: {}, projectId: {}", userId, projectId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ApiException("Project not found", 404));
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        ProjectDocument document = ProjectDocument.builder()
            .projectId(projectId)
            .memberId(member.getId())
            .title(request.getTitle())
            .content(request.getContent())
            .status(request.getStatus() != null ? request.getStatus() : DocStatus.DRAFT)
            .accessType(request.getAccessType() != null ? request.getAccessType() : DocAccessType.PRIVATE)
            .docType(request.getDocType() != null ? request.getDocType() : DocType.DOCUMENT)
            .leftMargin(request.getLeftMargin() != null ? request.getLeftMargin() : 56)
            .rightMargin(request.getRightMargin() != null ? request.getRightMargin() : 56)
            .assignees(new ArrayList<>())
            .comments(new ArrayList<>())
            .isDeleted(false)
            .build();
        
        ProjectDocument createdDocument = projectDocumentRepository.save(document);
        
        return Map.of("doc", DocumentResponseDto.fromEntity(createdDocument));
    }
    
    public Map<String, Object> updateDocument(User user, UpdateDocumentDto request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String docId = request.getDocId();
        
        log.info("Updating document - userId: {}, projectId: {}, docId: {}", userId, projectId, docId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ApiException("Project not found", 404));
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        ProjectDocument doc = projectDocumentRepository.findById(docId)
            .orElseThrow(() -> new ApiException("Document not found", 404));
        
        // Check permissions
        if (doc.getAccessType() == DocAccessType.PRIVATE && !doc.getMemberId().equals(member.getId())) {
            throw new ApiException("You do not have permission to update this document", 403);
        }
        
        // Update fields
        if (request.getTitle() != null) doc.setTitle(request.getTitle());
        if (request.getContent() != null) doc.setContent(request.getContent());
        if (request.getStatus() != null) doc.setStatus(request.getStatus());
        if (request.getAccessType() != null) doc.setAccessType(request.getAccessType());
        if (request.getDocType() != null) doc.setDocType(request.getDocType());
        if (request.getLeftMargin() != null) doc.setLeftMargin(request.getLeftMargin());
        if (request.getRightMargin() != null) doc.setRightMargin(request.getRightMargin());
        
        doc.setUpdatedAt(Instant.now());
        
        ProjectDocument updatedDocument = projectDocumentRepository.save(doc);
        
        return Map.of("doc", DocumentResponseDto.fromEntity(updatedDocument));
    }
    
    public Map<String, Object> deleteDocument(User user, DeleteDocumentDto request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String docId = request.getDocId();
        
        log.info("Deleting document - userId: {}, projectId: {}, docId: {}", userId, projectId, docId);
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        ProjectDocument doc = projectDocumentRepository.findById(docId)
            .orElseThrow(() -> new ApiException("Document not found", 404));
        
        // Check permissions - Note: In Node.js it's !== MEMBER, meaning OWNER/ADMIN can delete
        if (member.getRole() == MemberRole.MEMBER && !doc.getMemberId().equals(member.getId())) {
            throw new ApiException("You do not have permission to delete this document", 403);
        }
        
        projectDocumentRepository.delete(doc);
        
        return Map.of("docId", docId);
    }
    
    public Map<String, Object> assignMember(User user, AssignMemberDto request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String docId = request.getDocId();
        String memberId = request.getMemberId();
        
        log.info("Assigning member - userId: {}, docId: {}, memberId: {}", userId, docId, memberId);
        
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ApiException("Project not found", 404));
        
        ProjectDocument doc = projectDocumentRepository.findById(docId)
            .orElseThrow(() -> new ApiException("Document not found", 404));
        
        if (doc.getStatus() != DocStatus.PUBLISHED) {
            throw new ApiException("Cannot assign member to a non-published document", 400);
        }
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        // Check permissions
        if (!doc.getMemberId().equals(member.getId()) && member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("Only project owner, admins and creator can assign members to a document", 403);
        }
        
        Member assignedMember = memberRepository.findById(memberId)
            .orElseThrow(() -> new ApiException("Assignee not found", 404));
        
        if (doc.getAssignees().contains(memberId)) {
            throw new ApiException("Member is assigned already", 400);
        }
        
        doc.getAssignees().add(memberId);
        projectDocumentRepository.save(doc);
        
        // Send email notification
        String frontendDocumentLink = String.format("%s/dashboard/workspace/%s/docs/%s",
            frontendUrl, projectId, docId);
        
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("assignee", assignedMember.getEmail());
        templateVariables.put("link", frontendDocumentLink);
        templateVariables.put("projectName", project.getName());
        templateVariables.put("documentName", doc.getTitle());
        
        try {
            notificationService.sendWithTemplate(
                assignedMember.getEmail(),
                "📄 New Document Assigned to You - " + doc.getTitle(),
                "assigned-document",
                templateVariables
            );
        } catch (MessagingException e) {
            log.error("Failed to send document assignment email: {}", e.getMessage());
        }
        
        // Create comment
        Comment comment = Comment.builder()
            .content("Assigned member: " + assignedMember.getEmail())
            .memberId(member.getId())
            .commentType(CommentType.ASSIGNED_MEMBER)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        Comment createdComment = commentRepository.save(comment);
        doc.getComments().add(createdComment.getId());
        projectDocumentRepository.save(doc);
        
        return Map.of("docId", docId, "memberId", memberId);
    }
    
    public Map<String, Object> removeMember(User user, AssignMemberDto request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String docId = request.getDocId();
        String memberId = request.getMemberId();
        
        log.info("Removing assigned member - userId: {}, docId: {}, memberId: {}", userId, docId, memberId);
        
        ProjectDocument doc = projectDocumentRepository.findById(docId)
            .orElseThrow(() -> new ApiException("Document not found", 404));
        
        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
            .orElseThrow(() -> new ApiException("Member not found", 404));
        
        // Check permissions
        if (!doc.getMemberId().equals(member.getId()) && member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("Only project owner, admins and creator can remove assigned members to a document", 403);
        }
        
        Member assignedMember = memberRepository.findById(memberId)
            .orElseThrow(() -> new ApiException("Assignee member not found", 404));
        
        if (!doc.getAssignees().contains(memberId)) {
            throw new ApiException("Member is not assigned", 400);
        }
        
        doc.getAssignees().remove(memberId);
        projectDocumentRepository.save(doc);
        
        // Create comment
        Comment comment = Comment.builder()
            .content("Removed assigned member: " + assignedMember.getEmail())
            .memberId(member.getId())
            .commentType(CommentType.REMOVE_ASSIGNED_MEMBER)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
        
        Comment createdComment = commentRepository.save(comment);
        doc.getComments().add(createdComment.getId());
        projectDocumentRepository.save(doc);
        
        return Map.of("docId", docId, "memberId", memberId);
    }
}