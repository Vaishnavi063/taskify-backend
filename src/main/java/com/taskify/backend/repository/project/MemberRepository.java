package com.taskify.backend.repository.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    @Query("{ 'userId': ?0, 'projectId': ?1 }") // Corrected Query
    Optional<Member> findByUserIdAndProjectId(String userId, String projectId);

    List<Member> findByUserIdAndInvitationStatus(String userId, InvitationStatus status);
    Optional<Member> findByEmailAndProjectId(String email, String projectId);
    @Query("{ 'projectId': ?0 }")
    List<Member> findByProjectId(String projectId);

}