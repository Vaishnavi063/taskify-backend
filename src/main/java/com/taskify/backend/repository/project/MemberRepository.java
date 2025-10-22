package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {
}
