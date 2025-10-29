package com.taskify.backend.controllers.project;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.TaskService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.TaskValidator;
import com.taskify.backend.validators.project.UpdateMemberValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/project/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/createTask")
    public ApiResponse<Map<String,Object>> createTask(
            HttpServletRequest httpRequest,
            @Valid @RequestBody TaskValidator request
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = taskService.createTask(user,request);
        return ApiResponse.success(response, "Task created successfully", HttpStatus.OK.value());
    }
}
