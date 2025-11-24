package com.taskify.backend.repository.project;


import com.taskify.backend.models.project.Task;
import com.taskify.backend.validators.project.GetTasksValidator;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Repository
public interface TaskRepository extends MongoRepository<Task, String>, TaskRepositoryCustom {
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

    @Aggregation(pipeline = {
            // 1️⃣ Match: Only completed tasks for this project and month
            "{ $match: { " +
                    "projectId: ?0, " +
                    "status: 'COMPLETED', " +
                    "$expr: { " +
                    "  $and: [ " +
                    "    { $eq: [ { $month: '$completedDate' }, { $month: new Date() } ] }, " +
                    "    { $eq: [ { $year: '$completedDate' }, { $year: new Date() } ] } " +
                    "  ] " +
                    "} " +
                    "} }",

            // 2️⃣ Unwind assignees array (ignore empty)
            "{ $unwind: { path: '$assignees', preserveNullAndEmptyArrays: false } }",

            // 3️⃣ Group by assignee to count completed tasks
            "{ $group: { _id: '$assignees', completedTasksCount: { $sum: 1 } } }",

            // 4️⃣ Convert to ObjectId for lookups
            "{ $addFields: { " +
                    "memberIdString: '$_id', " +
                    "memberIdObj: { $convert: { input: '$_id', to: 'objectId', onError: null, onNull: null } } " +
                    "} }",

            // 5️⃣ Lookup member details
            "{ $lookup: { " +
                    "from: 'members', " +
                    "localField: 'memberIdObj', " +
                    "foreignField: '_id', " +
                    "as: 'memberDetails' " +
                    "} }",
            "{ $unwind: { path: '$memberDetails', preserveNullAndEmptyArrays: false } }",

            // 6️⃣ Convert userId string to ObjectId
            "{ $addFields: { " +
                    "userIdObj: { $convert: { input: '$memberDetails.userId', to: 'objectId', onError: null, onNull: null } } " +
                    "} }",

            // 7️⃣ Lookup user details
            "{ $lookup: { " +
                    "from: 'users', " +
                    "localField: 'userIdObj', " +
                    "foreignField: '_id', " +
                    "as: 'userDetails' " +
                    "} }",
            "{ $unwind: { path: '$userDetails', preserveNullAndEmptyArrays: false } }",

            // 8️⃣ Final projection (clean format)
            "{ $project: { " +
                    "_id: 0, " +
                    "memberId: '$memberIdString', " +
                    "completedTasksCount: 1, " +
                    "user: { " +
                    "fullName: '$userDetails.fullName', " +
                    "email: '$userDetails.email', " +
                    "avatar: '$userDetails.avatar', " +
                    "role: '$memberDetails.role' " +
                    "} " +
                    "} }",

            // 9️⃣ Sort by completed count descending
            "{ $sort: { completedTasksCount: -1 } }"
    })
    List<Map<String, Object>> getMembersCompletedTasksForCurrentMonth(String projectId);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "projectId: ?1, " +
                    "'assignees': ?0, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $group: { _id: '$status', count: { $sum: 1 } } }",
            "{ $project: { _id: 0, status: '$_id', count: 1 } }"
    })
    List<Map<String, Object>> getUserAssignedTasks(String memberId, String projectId);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "memberId: ?0, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $group: { _id: '$memberId', taskCount: { $sum: 1 } } }",
            "{ $project: { _id: 0, memberId: '$_id', taskCount: 1 } }"
    })
    Optional<Map<String, Object>> getUserCreatedTask(String memberId);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "'assignees': { $in: ?0 }, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $group: { _id: '$status', count: { $sum: 1 } } }",
            "{ $project: { _id: 0, status: '$_id', count: 1 } }"
    })
    List<Map<String, Object>> getUserAssignedTasksAllProjects(List<String> memberIds);

    @Aggregation(pipeline = {
            "{ $match: { " +
                    "memberId: { $in: ?0 }, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $group: { _id: null, taskCount: { $sum: 1 } } }",
            "{ $project: { _id: 0, taskCount: 1 } }"
    })
    Optional<Map<String, Object>> getUserCreatedTaskAllProjects(List<String> memberIds);

    // Single project
    @Aggregation(pipeline = {
            "{ $match: { " +
                    "projectId: ?1, " +
                    "status: 'COMPLETED', " +
                    "completedDate: { $gte: ?0 }, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $project: { completedDate: 1 } }"
    })
    List<Map<String, Object>> getLast30DaysTasksByProject(Date fromDate, String projectId);

    // All projects
    @Aggregation(pipeline = {
            "{ $match: { " +
                    "'assignees': { $in: ?1 }, " +
                    "status: 'COMPLETED', " +
                    "completedDate: { $gte: ?0 }, " +
                    "$or: [ { isDeleted: false }, { isDeleted: { $exists: false } } ] " +
                    "} }",
            "{ $project: { completedDate: 1 } }"
    })
    List<Map<String, Object>> getLast30DaysTasksAllProjects(Date fromDate, List<String> memberIds);


}
