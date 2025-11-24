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
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
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
    private final NotificationService notificationService;

    @Value("${frontend.url}")
    private String frontendUrl;

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

        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId());
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

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 404);
        }

        Optional<Map<String, Object>> taskOpt = taskRepository.getTaskWithComments(taskId, member.getId());
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
        List<Document> tasks = getTasksByProjectIdAndMemberId(projectId, memberId, query);

        log.info("Successfully fetched {} tasks for user {}", tasks.size(), userId);

        return Map.of(
                "tasks", tasks
        );
    }

    public List<Document> getTasksByProjectIdAndMemberId(String projectId, String memberId, GetTasksValidator filters) {

        String title = filters.getTitle();
        boolean createdByMe = filters.getCreatedByMe();
        boolean assignedToMe = filters.getAssignedToMe();
        String priority = filters.getPriority();
        String status = filters.getStatus();
        boolean sortByCreated = filters.getSortByCreated();

        Criteria criteria = Criteria.where("projectId").is(projectId)
                .and("isDeleted").is(false)
                .and("title").regex(title != null ? title : "", "i");

        if (createdByMe) {
            criteria.and("memberId").is(memberId);
        }

        if (assignedToMe) {
            criteria.and("assignees").in(memberId);
        }

        if (priority != null && !priority.isEmpty()) {
            criteria.and("priority").is(priority);
        }

        if (status != null && !status.isEmpty()) {
            criteria.and("status").is(status);
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),

                // Convert memberId to ObjectId for creator lookup
                Aggregation.addFields()
                        .addField("memberObjectId").withValue(
                                new Document("$convert", new Document("input", "$memberId")
                                        .append("to", "objectId")
                                        .append("onError", "$memberId")
                                )
                        ).build(),

                Aggregation.lookup("members", "memberObjectId", "_id", "creator"),
                Aggregation.unwind("creator", true),

                // Convert creator.userId to ObjectId for user lookup
                Aggregation.addFields()
                        .addField("userObjectId").withValue(
                                new Document("$convert", new Document("input", "$creator.userId")
                                        .append("to", "objectId")
                                        .append("onError", "$creator.userId")
                                )
                        ).build(),

                Aggregation.lookup("users", "userObjectId", "_id", "user"),
                Aggregation.unwind("user", true),

                // ✅ Convert assignees (array of Strings) → ObjectIds for lookup
                Aggregation.addFields()
                        .addField("assigneeObjectIds").withValue(
                                new Document("$map", new Document("input", "$assignees")
                                        .append("as", "id")
                                        .append("in", new Document("$convert", new Document("input", "$$id")
                                                .append("to", "objectId")
                                                .append("onError", "$$id")))
                                )
                        ).build(),

                // ✅ Lookup members using converted ObjectIds
                Aggregation.lookup("members", "assigneeObjectIds", "_id", "membersDetails"),

                // ✅ Map members to simplified structure
                Aggregation.addFields()
                        .addFieldWithValue("members",
                                new Document("$map", new Document("input", "$membersDetails")
                                        .append("as", "member")
                                        .append("in", new Document(
                                                "_id", new Document("$toString", "$$member._id"))
                                                .append("email", "$$member.email"))))
                        .addFieldWithValue("commentCount", new Document("$size", "$comments"))
                        .build(),

                Aggregation.project("projectId", "title", "description", "status", "priority",
                                "dueDate", "completedDate", "subTasks", "taskType", "taskNumber",
                                "isDeleted", "comments", "members", "commentCount", "createdAt")

                        .andExpression("{$toString: '$_id'}").as("_id")
                        .andExpression("{$toString: '$creator._id'}").as("creator.memberId")
                        .and("creator.email").as("creator.email")
                        .and("creator.role").as("creator.role")
                        .and("user.fullName").as("creator.fullName")
                        .and("user.avatar").as("creator.avatar"),

                Aggregation.sort(sortByCreated ? Sort.Direction.ASC : Sort.Direction.DESC, "createdAt"),

                // Add isMember flag
                Aggregation.addFields()
                        .addFieldWithValue("isMember",
                                new Document("$in", Arrays.asList(memberId, "$members._id")))
                        .build()
        );

        return mongoTemplate.aggregate(aggregation, "tasks", Document.class).getMappedResults();
    }

    public Map<String, Object> changeStatus(User user, ChangeStatusValidator body) {
        log.info("Changing status of task {} by user {}", body.getTaskId(), user.getId());

        String userId = user.getId();
        String taskId = body.getTaskId();
        TaskStatus newStatus = body.getStatus();

        // 1️⃣ Get existing task
        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, existingTask.getProjectId())
                .orElseThrow(() -> new ApiException("Member not found", 404));

        TaskStatus oldStatus = existingTask.getStatus();
        existingTask.setStatus(newStatus);

        if (TaskStatus.COMPLETED.equals(oldStatus)) {
            existingTask.setCompletedDate(LocalDate.now());
        }

        taskRepository.save(existingTask);

        Comment statusComment = new Comment();
        statusComment.setContent("Updated status: " + oldStatus + " → " + newStatus);
        statusComment.setMemberId(member.getId());
        statusComment.setCommentType(CommentType.STATUS_UPDATED);
        statusComment.setCreatedAt(Instant.now());

        Comment createdComment = commentRepository.save(statusComment);

        if (TaskStatus.COMPLETED.equals(newStatus)) {
            existingTask.setCompletedDate(LocalDate.now());
        } else {
            existingTask.setCompletedDate(null);
        }
        existingTask.getComments().add(createdComment.getId());

        taskRepository.save(existingTask);

        log.info("Status updated successfully for task {}: {} → {}", taskId, oldStatus, newStatus);

        return Map.of(
                "taskId", taskId
        );
    }

    public List<Map<String, Object>> getMembersCompletedTasks(User user, ValidateProjectIdQuery body) {
        log.info("user info {}", user);
        log.info("getMembersCompletedTasks {}", body);

        String projectId = body.getProjectId();

        projectRepository.findById(body.getProjectId())
                .orElseThrow(() -> new ApiException("Project not found", 404));

        return taskRepository.getMembersCompletedTasksForCurrentMonth(projectId);
    }

    public Map<String, Object> getUserAssignedTasks(User user, ValidateProjectIdQuery query) {
        String userId = user.getId();
        String projectId = query.getProjectId();

        log.info("getUserAssignedTasks called for userId={} projectId={}", userId, projectId);

        // If projectId is provided, fetch the member in that project
        Member member = null;
        if (projectId != null && !projectId.isEmpty()) {
            member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                    .orElseThrow(() -> new ApiException("Member not found for user in project", 404));
        }

        String memberId = member != null ? member.getId() : null;

        // Fetch assigned tasks
        List<Map<String, Object>> assignedTasks;
        if (memberId != null) {
            // If projectId is provided, filter by project
            assignedTasks = taskRepository.getUserAssignedTasks(memberId, projectId);
        } else {
            List<Member> members = memberRepository.findByUserIdAndInvitationStatus(userId, InvitationStatus.ACCEPTED);
            List<String> memberIds = members.stream().map(Member::getId).toList();
            assignedTasks = taskRepository.getUserAssignedTasksAllProjects(memberIds);
        }

        List<String> availableStatuses = List.of("TODO", "IN_PROGRESS", "UNDER_REVIEW", "COMPLETED");
        List<Map<String, Object>> tasksByStatus = availableStatuses.stream()
                .map(status -> {
                    Map<String, Object> found = assignedTasks.stream()
                            .filter(t -> t.get("status").equals(status))
                            .findFirst()
                            .orElse(Map.of("status", status, "count", 0));
                    return Map.of(
                            "status", found.get("status"),
                            "count", found.get("count")
                    );
                })
                .toList();

        // Tasks created by the user (all projects)
        Map<String, Object> createdTasks;
        if (memberId != null) {
            createdTasks = taskRepository.getUserCreatedTask(memberId).orElse(Map.of("taskCount", 0));
        } else {
            List<Member> members = memberRepository.findByUserIdAndInvitationStatus(userId, InvitationStatus.ACCEPTED);
            List<String> memberIds = members.stream().map(Member::getId).toList();
            createdTasks = taskRepository.getUserCreatedTaskAllProjects(memberIds).orElse(Map.of("taskCount", 0));
        }

        return Map.of(
                "tasks", tasksByStatus,
                "createdTasks", createdTasks
        );
    }

    public List<Map<String, Object>> getLast30DaysTasks(User user, ValidateProjectIdQuery query) {
        String userId = user.getId();
        String projectId = query.getProjectId();

        // 1️⃣ Calculate last 30 days range in UTC
        ZonedDateTime todayUTC = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime last30DaysUTC = todayUTC.minusDays(30);

        Date fromDate = Date.from(last30DaysUTC.toInstant());
        Date toDate = Date.from(todayUTC.toInstant());

        // 2️⃣ Fetch member(s)
        List<Member> members;
        if (projectId != null && !projectId.isEmpty()) {
            Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                    .orElseThrow(() -> new ApiException("Member not found for user in project", 404));
            members = List.of(member);
        } else {
            members = memberRepository.findByUserIdAndInvitationStatus(userId, InvitationStatus.ACCEPTED);
        }
        List<String> memberIds = members.stream().map(Member::getId).toList();

        // 3️⃣ Fetch tasks from repository
        List<Map<String, Object>> tasks;
        if (projectId != null && !projectId.isEmpty()) {
            tasks = taskRepository.getLast30DaysTasksByProject(fromDate, projectId);
        } else {
            tasks = taskRepository.getLast30DaysTasksAllProjects(fromDate, memberIds);
        }

        // 4️⃣ Prepare map for last 30 days
        Map<String, Long> resultMap = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            LocalDate date = last30DaysUTC.toLocalDate().plusDays(i);
            resultMap.put(date.toString(), 0L);
        }

        // 5️⃣ Count completed tasks per day (UTC)
        tasks.forEach(task -> {
            Date completedDate = (Date) task.get("completedDate");
            LocalDate dateUTC = completedDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            String key = dateUTC.toString();
            resultMap.put(key, resultMap.getOrDefault(key, 0L) + 1);
        });

        // 6️⃣ Convert map to list
        List<Map<String, Object>> resultList = resultMap.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "completedDate", entry.getKey(),
                        "count", entry.getValue()
                ))
                .toList();

        return resultList;
    }

    public Map<String, Object> assignMember(User user, AssignMemberValidator body){
        String userId = user.getId();
        String projectId = body.getProjectId();
        String taskId = body.getTaskId();
        String memberId = body.getMemberId();

        log.info("User info {}",user);
        log.info("Project info {}",body);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found",404));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found",404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found",404));

        if (member.getRole().equals(MemberRole.MEMBER)) {
            throw new ApiException("You are not allowed to assign members to tasks",403);
        }

        Member assignedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("Assignee not found",404));

        if (task.getAssignees() != null && task.getAssignees().contains(memberId)) {
            throw new ApiException("Member is assigned already",400);
        }

        if (task.getAssignees() == null) {
            task.setAssignees(new ArrayList<>());
        }
        task.getAssignees().add(memberId);
        taskRepository.save(task);

        String frontendTaskLink = String.format("%s/dashboard/workspace/%s/tasks/%s",
                frontendUrl, projectId, taskId);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("assignee", assignedMember.getEmail());
        templateVariables.put("projectAdminName", user.getFullName());
        templateVariables.put("link", frontendTaskLink);

        try {
            notificationService.sendWithTemplate(
                    assignedMember.getEmail(),
                    "New Task Assigned to You",
                    "assigned-task",
                    templateVariables
            );
        } catch (MessagingException e) {
            log.error("Failed to send assigned task email: {}", e.getMessage());
            throw new ApiException("Failed to send assigned task email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        Comment comment = new Comment();
        comment.setContent("Assigned member: " + assignedMember.getEmail());
        comment.setMemberId(member.getId());
        comment.setCommentType(CommentType.ASSIGNED_MEMBER);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        comment = commentRepository.save(comment);

        if (task.getComments() == null) {
            task.setComments(new ArrayList<>());
        }
        task.getComments().add(comment.getId());
        taskRepository.save(task);

        return Map.of(
                "taskId", taskId,
                "memberId", memberId
        );
    }

    public Map<String,Object> deleteTask(User user, ValidateTaskId query){
        String userId = user.getId();
        String projectId = query.getProjectId();
        String taskId = query.getTaskId();

        log.info("Deleting task: userId={}, taskId={}, projectId={}", userId, taskId, projectId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        if (member.getRole().equals(MemberRole.MEMBER)
                && !task.getMemberId().equals(member.getId())) {
            throw new ApiException("You are not allowed to delete this task", 403);
        }

        task.setIsDeleted(true);
        taskRepository.save(task);

        log.info("Task {} marked as deleted by user {}", taskId, userId);

        return Map.of(
                "taskId", taskId
        );
    }

    public Map<String, Object> removeAssignedMember(User user, AssignMemberValidator body) {
        String userId = user.getId();
        String projectId = body.getProjectId();
        String taskId = body.getTaskId();
        String memberId = body.getMemberId();

        log.info("Removing assigned member - userId: {}, taskId: {}, memberId: {}", userId, taskId, memberId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You are not allowed to assign members to tasks", 403);
        }

        Member assignedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("Assignee member not found", 404));

        if (!task.getAssignees().contains(memberId)) {
            throw new ApiException("Member is not assigned", 400);
        }

        task.getAssignees().remove(memberId);
        taskRepository.save(task);

        Comment comment = new Comment();
        comment.setContent("Removed assigned member: " + assignedMember.getEmail());
        comment.setMemberId(member.getId());
        comment.setCommentType(CommentType.REMOVE_ASSIGNED_MEMBER);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        comment = commentRepository.save(comment);

        task.getComments().add(comment.getId());
        taskRepository.save(task);

        return Map.of(
                "taskId", taskId,
                "memberId", memberId
        );
    }

    public Map<String, Object> addComment(User user, AddCommentValidator body) {
        String userId = user.getId();
        String taskId = body.getTaskId();
        String content = body.getContent();

        log.info("Adding comment - userId: {}, taskId: {}", userId, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId())
                .orElseThrow(() -> new ApiException("Member not found", 404));

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setMemberId(member.getId());
        comment.setCommentType(CommentType.GENERAL);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
        comment = commentRepository.save(comment);

        task.getComments().add(comment.getId());
        taskRepository.save(task);

        return Map.of("commentId", comment.getId());
    }

    public Map<String, Object> removeComment(User user, RemoveCommentValidator body) {
        String userId = user.getId();
        String taskId = body.getTaskId();
        String commentId = body.getCommentId();

        log.info("Removing comment - userId: {}, taskId: {}, commentId: {}", userId, taskId, commentId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId())
                .orElseThrow(() -> new ApiException("Member not found", 404));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comment not found", 404));

        if (member.getRole() == MemberRole.MEMBER && !member.getId().equals(comment.getMemberId())) {
            throw new ApiException("You have no permissions to remove this comment", 403);
        }

        task.getComments().remove(commentId);
        taskRepository.save(task);

        return Map.of("commentId", commentId);
    }

    public Map<String, Object> updateComment(User user, UpdateCommentValidator body) {
        String userId = user.getId();
        String taskId = body.getTaskId();
        String commentId = body.getCommentId();
        String content = body.getContent();

        log.info("Updating comment - userId: {}, taskId: {}, commentId: {}", userId, taskId, commentId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ApiException("Task not found", 404));

        Member member = memberRepository.findByUserIdAndProjectId(userId, task.getProjectId())
                .orElseThrow(() -> new ApiException("Member not found", 404));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException("Comment not found", 404));

        if (!member.getId().equals(comment.getMemberId()) || member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You do not have permission to update comment", 403);
        }

        comment.setContent(content);
        comment.setUpdatedAt(Instant.now());
        commentRepository.save(comment);

        return Map.of("commentId", commentId);
    }

    public Map<String, Object> getCompletedTasks(User user, GetTasksValidator query) {
        String userId = user.getId();
        String projectId = query.getProjectId();

        log.info("Getting completed tasks - userId: {}, projectId: {}", userId, projectId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", 404));

        return getCompletedTasksByProjectIdAndMemberId(projectId, member.getId(), query);
    }

    private Map<String, Object> getCompletedTasksByProjectIdAndMemberId(String projectId, String memberId, GetTasksValidator filters) {
        String title = filters.getTitle();
        boolean createdByMe = filters.getCreatedByMe();
        boolean assignedToMe = filters.getAssignedToMe();
        String priority = filters.getPriority();
        int page = filters.getPage();
        int limit = filters.getLimit();

        Criteria criteria = Criteria.where("projectId").is(projectId)
                .and("isDeleted").is(false)
                .and("status").is(TaskStatus.COMPLETED)
                .and("title").regex(title != null ? title : "", "i");

        if (createdByMe) {
            criteria.and("memberId").is(memberId);
        }

        if (assignedToMe) {
            criteria.and("assignees").in(memberId);
        }

        if (priority != null && !priority.isEmpty()) {
            criteria.and("priority").is(priority);
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),

                Aggregation.addFields()
                        .addField("memberObjectId").withValue(
                                new Document("$convert", new Document("input", "$memberId")
                                        .append("to", "objectId")
                                        .append("onError", "$memberId")
                                )
                        ).build(),

                Aggregation.lookup("members", "memberObjectId", "_id", "creator"),
                Aggregation.unwind("creator", true),

                Aggregation.addFields()
                        .addField("userObjectId").withValue(
                                new Document("$convert", new Document("input", "$creator.userId")
                                        .append("to", "objectId")
                                        .append("onError", "$creator.userId")
                                )
                        ).build(),

                Aggregation.lookup("users", "userObjectId", "_id", "user"),
                Aggregation.unwind("user", true),

                Aggregation.addFields()
                        .addField("assigneeObjectIds").withValue(
                                new Document("$map", new Document("input", "$assignees")
                                        .append("as", "id")
                                        .append("in", new Document("$convert", new Document("input", "$id")
                                                .append("to", "objectId")
                                                .append("onError", "$id")))
                                )
                        ).build(),

                Aggregation.lookup("members", "assigneeObjectIds", "_id", "membersDetails"),

                Aggregation.addFields()
                        .addFieldWithValue("members",
                                new Document("$map", new Document("input", "$membersDetails")
                                        .append("as", "member")
                                        .append("in", new Document(
                                                "_id", new Document("$toString", "$member._id"))
                                                .append("email", "$member.email"))))
                        .addFieldWithValue("commentCount", new Document("$size", "$comments"))
                        .build(),

                Aggregation.project("projectId", "title", "description", "status", "priority",
                                "dueDate", "completedDate", "subTasks", "taskType", "taskNumber",
                                "isDeleted", "comments", "members", "commentCount")
                        .andExpression("{$toString: '$_id'}").as("_id")
                        .andExpression("{$toString: '$creator._id'}").as("creator.memberId")
                        .and("creator.email").as("creator.email")
                        .and("creator.role").as("creator.role")
                        .and("user.fullName").as("creator.fullName")
                        .and("user.avatar").as("creator.avatar"),

                Aggregation.sort(Sort.Direction.DESC, "completedDate"),

                Aggregation.addFields()
                        .addFieldWithValue("isMember",
                                new Document("$in", Arrays.asList(memberId, "$members._id")))
                        .build()
        );

        List<Document> tasks = mongoTemplate.aggregate(aggregation, "tasks", Document.class).getMappedResults();

        int total = tasks.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int start = (page - 1) * limit;
        int end = Math.min(start + limit, total);
        List<Document> paginatedTasks = start < total ? tasks.subList(start, end) : List.of();

        Map<String, Object> result = new HashMap<>();
        result.put("tasks", paginatedTasks);
        result.put("total", total);
        result.put("limit", limit);
        result.put("page", page);
        result.put("totalPages", totalPages);
        result.put("serialNumberStartFrom", start + 1);
        result.put("hasPrevPage", page > 1);
        result.put("hasNextPage", page < totalPages);
        result.put("prevPage", page > 1 ? page - 1 : null);
        result.put("nextPage", page < totalPages ? page + 1 : null);

        return result;
    }
}