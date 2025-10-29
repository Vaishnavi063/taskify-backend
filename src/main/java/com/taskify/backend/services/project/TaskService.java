package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.models.project.Task;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.repository.project.TaskRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.TaskValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;


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

}
