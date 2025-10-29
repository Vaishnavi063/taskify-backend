package com.taskify.backend.repository.project;


import com.taskify.backend.models.project.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    Optional<Task> findTopByProjectIdOrderByTaskNumberDesc(String projectId);
}
