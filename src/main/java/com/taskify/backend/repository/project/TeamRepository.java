package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends MongoRepository<Team, String> {
    Optional<Team> findByNameAndProjectId(String name, String projectId);
}

