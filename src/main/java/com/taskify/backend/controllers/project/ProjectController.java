package com.taskify.backend.controllers.project;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.ProjectService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.ProjectIdQueryValidator;
import com.taskify.backend.validators.project.ProjectValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/getProject")
    public ApiResponse<Map<String, Object>> getProject(
            HttpServletRequest request,
            @Valid @ModelAttribute ProjectIdQueryValidator query
    ) {
        User user = (User) request.getAttribute("user");

        Map<String, Object> response = projectService.getProject(user, query);

        return ApiResponse.success(response, "Project details successfully fetched", HttpStatus.OK.value());
    }

    @PostMapping("/createProject")
    public ApiResponse<Map<String, Object>> createProject(HttpServletRequest request, @Valid @RequestBody ProjectValidator project){
        User user = (User) request.getAttribute("user");
        System.out.println("USER ::" + user);
        Map<String ,Object> response = projectService.createProject(user,project);
        return ApiResponse.success(response, "Project created successfully", HttpStatus.OK.value());
    }
}
