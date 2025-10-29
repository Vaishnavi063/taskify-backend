package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Label;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.repository.project.LabelRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.ProjectIdQueryValidator;
import com.taskify.backend.validators.project.ProjectIdValidator;
import com.taskify.backend.validators.project.ProjectValidator;
import com.taskify.backend.validators.project.UpdateProjectValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final LabelRepository labelRepository;

    public Map<String, Object> getProject(User user, ProjectIdQueryValidator query) {
        log.info("User Info :: {}", user);
        log.info("Project Query Info :: {}", query);

        String userId = user.getId();
        String projectId = query.getProjectId();

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.BAD_REQUEST.value()));

        if (!InvitationStatus.ACCEPTED.equals(member.getInvitationStatus())) {
            throw new ApiException("You are not a member of this project", HttpStatus.BAD_REQUEST.value());
        }

        Project project = projectRepository.findById(member.getProjectId())
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        User owner = userRepository.findById(project.getUserId())
                .orElseThrow(() -> new ApiException("Owner not found", HttpStatus.NOT_FOUND.value()));

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("projectId", project.getId());
        projectData.put("memberId", member.getId());
        projectData.put("name", project.getName());
        projectData.put("description", project.getDescription());
        projectData.put("tags", project.getTags());
        projectData.put("isDeleted", project.isDeleted());
        projectData.put("role", member.getRole());

        Map<String, Object> ownerData = new HashMap<>();
        ownerData.put("fullName", owner.getFullName());
        ownerData.put("email", owner.getEmail());
        ownerData.put("avatar", owner.getAvatar());
        projectData.put("owner", ownerData);

        return Map.of("project", projectData);
    }

    public Map<String, Object> createProject(User user, ProjectValidator project) {
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
                    throw new ApiException("You can only create 10 projects. Please upgrade your plan.", HttpStatus.BAD_REQUEST.value());
                break;
            case ENTERPRISE:
                if (allProjects.size() >= 25)
                    throw new ApiException("You can only create 25 projects. Please contact support.", HttpStatus.BAD_REQUEST.value());
                break;
            default:
                throw new ApiException("Invalid pricing model.", HttpStatus.BAD_REQUEST.value());
        }

        // ✅ Store only userId as String
        Project newProject = Project.builder()
                .name(project.getName())
                .description(project.getDescription())
                .userId(userId)  // Store only the ID string
                .tags(project.getTags() != null ? project.getTags() : new ArrayList<>())
                .isDeleted(false)
                .build();

        newProject = projectRepository.save(newProject);

        createDefaultLabels(newProject);

        // ✅ Store only IDs as Strings
        Member owner = Member.builder()
                .userId(userId)  // Store only the ID string
                .projectId(newProject.getId())  // Store only the ID string
                .email(existingUser.getEmail())
                .invitationStatus(InvitationStatus.ACCEPTED)
                .role(MemberRole.OWNER)
                .build();
        memberRepository.save(owner);

        return Map.of("project", newProject);
    }

    private void createDefaultLabels(Project project) {
        Instant now = Instant.now();

        List<Label> defaultLabels = List.of(
                new Label(null, project.getId(), "feature", "A new capability, functionality, or enhancement to be added.", "#2b90d9", now, now),
                new Label(null, project.getId(), "bug", "A defect or error that causes incorrect or unexpected behavior.", "#e74c3c", now, now),
                new Label(null, project.getId(), "improvement", "Enhancing an existing feature or optimizing performance.", "#27ae60", now, now),
                new Label(null, project.getId(), "documentation", "Writing or updating technical or user documentation.", "#f39c12", now, now),
                new Label(null, project.getId(), "test", "Creating or updating test cases, QA tasks, or validation work.", "#8e44ad", now, now),
                new Label(null, project.getId(), "design", "UI/UX design tasks, including wireframes, mockups, or user flows.", "#e67e22", now, now),
                new Label(null, project.getId(), "research", "Investigation or exploration to inform future work or decision-making.", "#16a085", now, now),
                new Label(null, project.getId(), "refactor", "Code cleanup or restructuring without changing functionality.", "#95a5a6", now, now),
                new Label(null, project.getId(), "maintenance", "Routine system upkeep, such as dependency updates or server patches.", "#7f8c8d", now, now),
                new Label(null, project.getId(), "deployment", "Tasks related to releasing or deploying software to environments.", "#34495e", now, now),
                new Label(null, project.getId(), "task", "A general-purpose task that doesn't fit other categories.", "#bdc3c7", now, now),
                new Label(null, project.getId(), "discussion", "Conversations or decision-making items not tied to direct implementation.", "#9b59b6", now, now),
                new Label(null, project.getId(), "blocked", "Indicates a task is currently blocked by another issue or dependency.", "#c0392b", now, now),
                new Label(null, project.getId(), "urgent", "High-priority task requiring immediate attention.", "#d35400", now, now),
                new Label(null, project.getId(), "review", "Tasks involving code or design review.", "#2980b9", now, now),
                new Label(null, project.getId(), "security", "Tasks related to fixing vulnerabilities or improving security posture.", "#e84393", now, now)
        );

        labelRepository.saveAll(defaultLabels);
    }

    public Map<String, Object> updateProject(User user, UpdateProjectValidator project) {
        String projectId = project.get_id();
        String userId = user.getId();

        log.info("Fetching project - projectId: {}", projectId);

        Project existedProject = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "Project not found",
                        HttpStatus.NOT_FOUND.value()
                ));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException(
                        "Member not found",
                        HttpStatus.BAD_REQUEST.value()
                ));

        if (!MemberRole.OWNER.equals(member.getRole())) {
            throw new ApiException(
                    "You are not allowed to edit this project",
                    HttpStatus.FORBIDDEN.value()
            );
        }

        existedProject.setName(project.getName());
        existedProject.setDescription(project.getDescription());
        existedProject.setTags(project.getTags());

        log.info("Saving updated project - projectId: {}", projectId);

        Project updatedProject = projectRepository.save(existedProject);

        Map<String, Object> response = new HashMap<>();
        response.put("project", Map.of(
                "id", updatedProject.getId(),
                "name", updatedProject.getName(),
                "description", updatedProject.getDescription(),
                "tags", updatedProject.getTags()
        ));

        return response;
    }

    public Map<String, Object> deleteProject(User user, ProjectIdValidator projectIdData) {
        String userId = user.getId();
        String projectId = projectIdData.get_id();

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        if (project.isDeleted()) {
            throw new ApiException("Project already deleted", HttpStatus.BAD_REQUEST.value());
        }

        if (!project.getUserId().equals(userId)) {
            throw new ApiException("You are not allowed to delete this project", HttpStatus.FORBIDDEN.value());
        }

        project.setDeleted(true);
        projectRepository.save(project);

        List<Member> members = memberRepository.findByProjectId(projectId);

        members.forEach(member -> {
            member.setInvitationStatus(InvitationStatus.REJECTED);
            memberRepository.save(member);
        });

        return Map.of("projectId", projectId);
    }

    public Map<String, Object> getProjects(User user) {
        String userId = user.getId();
        log.info("Fetching projects - userId: {}", userId);

        List<Member> members = memberRepository.findByUserIdAndInvitationStatus(
                userId,
                InvitationStatus.ACCEPTED
        );

        log.info("Members found: {}", members.size());

        List<Map<String, Object>> projects = members.stream()
                .map(member -> {
                    Project project = projectRepository.findById(member.getProjectId())
                            .orElse(null);

                    if (project == null || project.isDeleted()) {
                        return null;
                    }

                    User owner = userRepository.findById(project.getUserId())
                            .orElse(null);

                    Map<String, Object> projectData = new HashMap<>();
                    projectData.put("projectId", project.getId());
                    projectData.put("memberId", member.getId());
                    projectData.put("name", project.getName());
                    projectData.put("description", project.getDescription());
                    projectData.put("tags", project.getTags());
                    projectData.put("isDeleted", project.isDeleted());
                    projectData.put("role", member.getRole());

                    Map<String, Object> ownerData = new HashMap<>();
                    if (owner != null) {
                        ownerData.put("fullName", owner.getFullName());
                        ownerData.put("email", owner.getEmail());
                        ownerData.put("avatar", owner.getAvatar());
                    } else {
                        ownerData.put("fullName", "Unknown");
                        ownerData.put("email", "N/A");
                        ownerData.put("avatar", null);
                    }
                    projectData.put("owner", ownerData);

                    return projectData;
                })
                .filter(Objects::nonNull)
                .toList();

        log.info("Projects after filtering: {}", projects.size());
        return Map.of("projects", projects);
    }
}