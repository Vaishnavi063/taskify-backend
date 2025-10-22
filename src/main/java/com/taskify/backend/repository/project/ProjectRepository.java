package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Project;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    List<Project> findByUserId(String userId);
    List<Project> findByIsDeletedFalse();
}

