package com.taskify.backend.repository.project;

import com.taskify.backend.dto.Document.GetDocumentsQueryDto;

import java.util.Map;
import java.util.Optional;

public interface ProjectDocumentRepositoryCustom {
    
    Optional<Map<String, Object>> getFullDocumentById(String docId, String memberId);
    
    Map<String, Object> getDocuments(String projectId, String memberId, GetDocumentsQueryDto filters);
}