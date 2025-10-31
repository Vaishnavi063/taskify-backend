package com.taskify.backend.services.project;

import com.taskify.backend.constants.CommentEnums.CommentType;
import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.constants.TaskEnums.TaskStatus;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Comment;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.models.project.Task;
import com.taskify.backend.repository.project.*;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.GetTaskQueryValidator;
import com.taskify.backend.validators.project.GetTasksValidator;
import com.taskify.backend.validators.project.TaskValidator;
import com.taskify.backend.validators.project.UpdateTaskValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;
    private final MongoTemplate mongoTemplate;


    public Map<String, Object> createTask(User user, TaskValidator task) {
        String userId = user.getId();
        log.info("Creating task for user {}", userId);
        log.info("Creating task task {}", task);

        Optional<Project> projectOpt = projectRepository.findById(task.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 404);
        }

        Project project = projectOpt.get();
        log.info("Creating task project {}", project);

        Optional<Member>  memberOpt = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId());
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }

        Member member = memberOpt.get();
        log.info("Creating task member {}", member);
        if(!member.getInvitationStatus().equals(InvitationStatus.ACCEPTED)){
            throw new ApiException("Your are not allowed to create a task", 403);
        }

        Integer nextTaskNumber = getTaskNumber(task.getProjectId());
        log.info("Next task number for project {}: {}", task.getProjectId(), nextTaskNumber);

        Task newTask = Task.builder()
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .projectId(task.getProjectId())
                .userId(userId)
                .taskType(task.getTaskType())
                .taskNumber(nextTaskNumber)
                .memberId(member.getId())
                .build();
        taskRepository.save(newTask);
        return Map.of(
                "taskId", newTask
        );
    }

    public Integer getTaskNumber(String projectId) {
        Optional<Task> latestTask = taskRepository.findTopByProjectIdOrderByTaskNumberDesc(projectId);

        return latestTask
                .map(task -> task.getTaskNumber() + 1)
                .orElse(1);
    }

    public Map<String,Object> updateTask(User user, UpdateTaskValidator task) {
        String userId = user.getId();
        String fullName = user.getFullName();
        String taskId = task.getTaskId();

        log.info("Updating task {} for user {}", taskId, userId);
        log.info("Updating task {} ", task);

        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId());
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }
        Member member = memberOpt.get();

        Optional<Task> existingTaskOpt = taskRepository.findById(taskId);
        if(existingTaskOpt.isEmpty()) {
            throw new ApiException("Task not found", 404);
        }
        Task existingTask = existingTaskOpt.get();

        if(member.getRole().equals(MemberRole.MEMBER) && !member.getId().equals(existingTask.getMemberId())) {
            throw new ApiException("You are not allowed to update this task", 403);
        }

        if (task.getStatus() == TaskStatus.COMPLETED) {
            task.setCompletedDate(LocalDate.now());
        }
        Task updatedTask = taskRepository.save(Task.builder()
                .id(taskId)
                .title(task.getTitle() != null ? task.getTitle() : existingTask.getTitle())
                .description(task.getDescription() != null ? task.getDescription() : existingTask.getDescription())
                .status(task.getStatus() != null ? task.getStatus() : existingTask.getStatus())
                .priority(task.getPriority() != null ? task.getPriority() : existingTask.getPriority())
                .dueDate(task.getDueDate() != null ? task.getDueDate() : existingTask.getDueDate())
                .subTasks(task.getSubTasks() != null ? task.getSubTasks() : existingTask.getSubTasks())
                .completedDate(task.getCompletedDate() != null ? task.getCompletedDate() : existingTask.getCompletedDate())
                .memberId(existingTask.getMemberId())
                .projectId(existingTask.getProjectId())
                .build());


        boolean isSubtaskAdded = task.getSubTasks() != null &&
                task.getSubTasks().size() != (existingTask.getSubTasks() != null
                        ? existingTask.getSubTasks().size()
                        : 0);


        if (!isSubtaskAdded) {
            Comment comment = new Comment();
            comment.setContent("Task updated by " + fullName);
            comment.setMemberId(member.getId());
            comment.setCommentType(CommentType.COMMENT_UPDATED);

            Comment createdComment = commentRepository.save(comment);

            List<String> comments = updatedTask.getComments();
            if (comments == null) {
                comments = new ArrayList<>();
            }
            comments.add(createdComment.getId());
            updatedTask.setComments(comments);

            taskRepository.save(updatedTask);
        }

        log.info("Task {} updated successfully by {}", taskId, fullName);

        return Map.of(
                "status", 200,
                "message", "Task updated successfully",
                "task", updatedTask
        );
    }

    public Map<String,Object> getTask(User user, GetTaskQueryValidator query){
        String userId = user.getId();
        String projectId = query.getProjectId();
        String taskId = query.getTaskId();
        log.info("Getting task {} for user {} project {}", taskId, userId, projectId);

        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(userId, projectId);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }
        Member member = memberOpt.get();

        Optional<Project>  projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 404);
        }

        Optional<Map<String, Object>> taskOpt = taskRepository.getTaskByIdAndMemberId(taskId,member.getId());
        if (taskOpt.isEmpty()) {
            throw new ApiException("Task not found", 404);
        }
        Map<String, Object> task = taskOpt.get();

        return Map.of(
                "task", task
        );
    }

    public Map<String, Object> getTasks(User user, GetTasksValidator query) {
        String userId = user.getId();
        String projectId = query.getProjectId();

        log.info("Getting tasks for user {} project {}", userId, projectId);
        log.info("Query filters: {}", query);

        // Validate user is a member of the project
        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(userId, projectId);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }
        Member member = memberOpt.get();
        String memberId = member.getId();

        // Fetch tasks with applied filters
        List<Map<String, Object>> tasks = getTasksByProjectIdAndMemberId(projectId, memberId, query);

        log.info("Successfully fetched {} tasks for user {}", tasks.size(), userId);

        return Map.of(
                "tasks", tasks,
                "count", tasks.size()
        );
    }

    /**
     * 🔧 Core method for fetching tasks with MongoDB aggregation pipeline
     * Applies multiple filters, lookups, and transformations
     *
     * Pipeline stages:
     * 1. Match - Filter by project and criteria
     * 2. Convert - Convert string IDs to ObjectId
     * 3. Lookup - Join with members and users collections
     * 4. Unwind - Flatten arrays
     * 5. AddFields - Enrich with calculated fields
     * 6. Project - Select final fields
     * 7. Sort - Order results
     */
    /**
     * 🔧 Core aggregation pipeline for fetching tasks
     */
    public List<Map<String, Object>> getTasksByProjectIdAndMemberId(
            String projectId, String memberId, GetTasksValidator query) {

        List<AggregationOperation> operations = new ArrayList<>();

        // ✅ 1️⃣ MATCH STAGE
        Criteria criteria = Criteria.where("projectId").is(projectId)
                .orOperator(
                        Criteria.where("isDeleted").is(false),
                        Criteria.where("isDeleted").exists(false)
                );

        if (query.getStatus() != null && !query.getStatus().isEmpty()) {
            criteria.and("status").is(query.getStatus());
        }
        if (query.getPriority() != null && !query.getPriority().isEmpty()) {
            criteria.and("priority").is(query.getPriority());
        }
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            criteria.and("title").regex(query.getTitle(), "i");
        }
        if (query.isCreatedByMe()) {
            criteria.and("userId").is(memberId);
        }
        if (query.isAssignedToMe()) {
            criteria.and("memberId").is(memberId);
        }

        operations.add(Aggregation.match(criteria));

        // ✅ 2️⃣ ADD FIELDS - Convert IDs to ObjectId
        operations.add(Aggregation.addFields()
                .addFieldWithValue("memberIdObj",
                        ConvertOperators.ToObjectId.toObjectId("$memberId"))
                .addFieldWithValue("userIdObj",
                        ConvertOperators.ToObjectId.toObjectId("$userId"))
                .build());

        // ✅ 3️⃣ LOOKUP - Members
        operations.add(Aggregation.lookup("members", "memberIdObj", "_id", "memberDetails"));
        operations.add(Aggregation.unwind("memberDetails", true));

        // ✅ 4️⃣ LOOKUP - Users (for creator info)
        operations.add(Aggregation.addFields()
                .addFieldWithValue("creatorUserIdObj",
                        ConvertOperators.ToObjectId.toObjectId("$memberDetails.userId"))
                .build());
        operations.add(Aggregation.lookup("users", "creatorUserIdObj", "_id", "creatorUserDetails"));
        operations.add(Aggregation.unwind("creatorUserDetails", true));

        // ✅ 5️⃣ LOOKUP - Assignees
        operations.add(Aggregation.lookup("members", "assignees", "_id", "membersDetails"));

        // ✅ 6️⃣ ADD FIELDS - Construct creator object BEFORE projection
        // 🔴 FIX: Build the creator object in addFields() stage
        operations.add(Aggregation.addFields()
                .addFieldWithValue("creator",
                        new Document()
                                .append("memberId", new Document("$toString", "$memberDetails._id"))
                                .append("email", "$memberDetails.email")
                                .append("role", "$memberDetails.role")
                                .append("fullName", "$creatorUserDetails.fullName")
                )
                .addFieldWithValue("members",
                        ArrayOperators.ArrayElemAt.arrayOf("$membersDetails").elementAt(0))
                .addFieldWithValue("commentCount",
                        ArrayOperators.Size.lengthOfArray(
                                ConditionalOperators.ifNull("$comments").then(Collections.emptyList())))
                .build());

        // ✅ 7️⃣ PROJECT - Select final fields
        operations.add(Aggregation.project()
                .and(ConvertOperators.ToString.toString("$_id")).as("_id")
                .andInclude("projectId", "title", "description", "status", "priority",
                        "taskType", "taskNumber", "isDeleted", "dueDate", "tags",
                        "completedDate", "subTasks", "creator", "members", "commentCount"));

        // ✅ 8️⃣ SORT
        operations.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")));

        Aggregation aggregation = Aggregation.newAggregation(operations);

        log.info("Executing aggregation pipeline with {} operations", operations.size());

        List<Map<String, Object>> results = (List<Map<String, Object>>) (List<?>)
                mongoTemplate.aggregate(aggregation, "tasks", Map.class).getMappedResults();

        log.info("Aggregation completed. Retrieved {} results", results.size());

        return results;
    }

}
