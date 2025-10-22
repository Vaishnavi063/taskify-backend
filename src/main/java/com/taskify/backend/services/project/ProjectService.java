package com.taskify.backend.services.project;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.ProjectIdQueryValidator;
import com.taskify.backend.validators.project.ProjectValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public Map<String,Object> getProject(User user, ProjectIdQueryValidator query) {
        log.info("User Info :: {}", user);
        log.info("Project Query Info :: {}", query);

        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return Map.of();
    }

    public Map<String,Object> createProject(User user, ProjectValidator project) {
        log.info("User Info :: {}", user);
        log.info("Project Query Info :: {}", project);

        String userId = user.getId();

        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Project> allProjects = projectRepository.findByUserId(userId);


        switch (user.getPricingModel()) {
            case FREE:
                if (allProjects.size() >= 1)
                    throw new ApiException("You can only create one project. Please upgrade your plan.", HttpStatus.BAD_REQUEST.value());
                break;
            case PREMIUM:
                if (allProjects.size() >= 10)
                    throw new ApiException("You can only create 10 project. Please upgrade your plan.", HttpStatus.BAD_REQUEST.value());
                break;
            case ENTERPRISE:
                if (allProjects.size() >= 25)
                    throw new ApiException("You can only create 25 project. Please upgrade your plan.", HttpStatus.BAD_REQUEST.value());
                break;
            default:
                throw new ApiException("Invalid pricing model.", HttpStatus.BAD_REQUEST.value());
        }

        List<String> tags = project.getTags() != null
                ? project.getTags()
                : new ArrayList<>();

        Project newProject = Project.builder()
                .name(project.getName())
                .description(project.getDescription())
                .userId(userId)
                .isDeleted(false)
                .tags(tags)
                .build();

        newProject = projectRepository.save(newProject);

        return Map.of("project", newProject);
    }
}
