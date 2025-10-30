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
import com.taskify.backend.repository.project.CommentRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.repository.project.TaskRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.GetTaskQueryValidator;
import com.taskify.backend.validators.project.TaskValidator;
import com.taskify.backend.validators.project.UpdateTaskValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final CommentRepository commentRepository;


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
}
