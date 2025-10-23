package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Member;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    Optional<Member> findByUserIdAndProjectId(String userId, String projectId);

    @Aggregation(pipeline = {
            "{ $match: { '_id': ?0 } }",
            "{ $lookup: { from: 'projects', localField: 'projectId', foreignField: '_id', as: 'project' } }",
            "{ $unwind: '$project' }",
            "{ $lookup: { from: 'users', localField: 'userId', foreignField: '_id', as: 'user' } }",
            "{ $unwind: '$user' }",
            "{ $project: { " +
                    "'_id': 0, " +
                    "'projectId': '$project._id', " +
                    "'memberId': '$_id', " +
                    "'name': '$project.name', " +
                    "'description': '$project.description', " +
                    "'tags': '$project.tags', " +
                    "'isDeleted': '$project.isDeleted', " +
                    "'role': '$role', " +
                    "'owner': { " +
                    "'fullName': '$user.fullName', " +
                    "'email': '$user.email', " +
                    "'avatar': '$user.avatar' " +
                    "} " +
                    "} }"
    })
    Optional<Map<String, Object>> getProjectByMemberId(String memberId);
}