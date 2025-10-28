package com.taskify.backend.repository.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    @Query("{ 'userId.$id': ?0, 'projectId.$id': ?1 }")
    Optional<Member> findByUserIdAndProjectId(String userId, String projectId);

    List<Member> findByUserIdAndInvitationStatus(String userId, InvitationStatus status);

    @Query("{ 'projectId.$id': ?0 }")
    List<Member> findByProjectId_Id(String projectId);

}