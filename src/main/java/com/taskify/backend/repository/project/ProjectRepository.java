package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findByUserId(String userId);
    List<Project> findByIsDeletedFalse();
    Optional<Project> findByIdAndIsDeletedFalse(String projectId);
    Optional<Project> findByIdAndUserId(String projectId, String userId);
    List<Project> findByUserIdAndIsDeletedFalse(String userId);
}

