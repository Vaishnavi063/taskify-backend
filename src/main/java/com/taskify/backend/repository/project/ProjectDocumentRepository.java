package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.ProjectDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDocumentRepository extends MongoRepository<ProjectDocument, String> {
}
