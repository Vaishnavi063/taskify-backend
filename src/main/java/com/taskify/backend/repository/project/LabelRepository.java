package com.taskify.backend.repository.project;


import com.taskify.backend.models.project.Label;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends MongoRepository<Label, String> {
}

