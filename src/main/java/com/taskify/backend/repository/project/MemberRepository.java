package com.taskify.backend.repository.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    Optional<Member> findByUserIdAndProjectId(String userId, String projectId);

    @Aggregation(pipeline = {
            "{ $match: { 'userId.$id': ?0, 'invitationStatus': 'ACCEPTED' } }",
            "{ $lookup: { from: 'projects', localField: 'projectId.$id', foreignField: '_id', as: 'project' } }",
            "{ $unwind: '$project' }",
            "{ $match: { 'project.isDeleted': { $ne: true } } }",
            "{ $lookup: { from: 'users', localField: 'project.userId.$id', foreignField: '_id', as: 'user' } }",
            "{ $unwind: '$user' }",
            "{ $project: { " +
                    "'_id': 0, " +
                    "'memberId': '$_id', " +
                    "'projectId': '$project._id', " +
                    "'name': '$project.name', " +
                    "'description': '$project.description', " +
                    "'tags': '$project.tags', " +
                    "'isDeleted': '$project.isDeleted', " +
                    "'role': 1, " +
                    "'owner': { " +
                    "'fullName': '$user.fullName', " +
                    "'email': '$user.email', " +
                    "'avatar': '$user.avatar' " +
                    "} " +
                    "} }"
    })
    List<Map<String, Object>> findProjectsByUserId(String userId);
    List<Member> findByProjectId_Id(String projectId);
    List<Member> findByUserIdAndInvitationStatus(User user, InvitationStatus status);
}