package com.taskify.backend.repository.project;


import com.taskify.backend.models.project.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
}
