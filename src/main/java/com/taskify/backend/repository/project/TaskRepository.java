package com.taskify.backend.repository.project;


import com.taskify.backend.models.project.Task;
import com.taskify.backend.validators.project.GetTasksValidator;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    Optional<Task> findTopByProjectIdOrderByTaskNumberDesc(String projectId);

    @Aggregation(pipeline = {
            "{ $match: { _id: ObjectId(?0), $or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] } }",
            "{ $addFields: { " +
                    "memberIdObj: { $convert: { input: '$memberId', to: 'objectId', onError: null, onNull: null } }, " +
                    "assigneesObj: { $map: { input: '$assignees', as: 'a', in: { $convert: { input: '$$a', to: 'objectId', onError: null, onNull: null } } } } " +
                    "} }",
            "{ $lookup: { from: 'members', localField: 'memberIdObj', foreignField: '_id', as: 'member' } }",
            "{ $unwind: { path: '$member', preserveNullAndEmptyArrays: true } }",
            "{ $lookup: { from: 'users', localField: 'member.userId', foreignField: '_id', as: 'user' } }",
            "{ $unwind: { path: '$user', preserveNullAndEmptyArrays: true } }",
            "{ $lookup: { from: 'members', localField: 'assigneesObj', foreignField: '_id', as: 'membersDetails' } }",
            "{ $addFields: { " +
                    "members: { $map: { input: '$membersDetails', as: 'm', in: { _id: { $toString: '$$m._id' }, email: '$$m.email' } } }, " +
                    "isCreator: { $eq: ['$memberIdObj', ObjectId(?1)] } " +
                    "} }",
            "{ $project: { " +
                    "_id: { $toString: '$_id' }, " +
                    "title: 1, " +
                    "description: 1, " +
                    "status: 1, " +
                    "priority: 1, " +
                    "dueDate: 1, " +
                    "comments: { $ifNull: ['$comments', []] }, " +
                    "members: 1, " +
                    "isCreator: 1, " +
                    "creator: { " +
                    "fullName: '$user.fullName', " +
                    "email: '$member.email', " +
                    "role: '$member.role', " +
                    "avatar: { url: '$user.avatar' } " +
                    "} " +
                    "} }"
    })
    Optional<Map<String, Object>> getTaskByIdAndMemberId(String taskId, String memberId);
}
