package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.ProjectDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDocumentRepository extends MongoRepository<ProjectDocument, String>, ProjectDocumentRepositoryCustom {
    
    List<ProjectDocument> findByProjectId(String projectId);
    
    Optional<ProjectDocument> findByIdAndIsDeletedFalse(String id);
}
