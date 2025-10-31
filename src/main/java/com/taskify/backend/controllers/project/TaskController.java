package com.taskify.backend.controllers.project;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.TaskService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.*;
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

    @PatchMapping("/updateTask")
    public ApiResponse<Map<String,Object>> updateTask(
            HttpServletRequest httpRequest,
             @Valid @RequestBody UpdateTaskValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.updateTask(user,body);
        return ApiResponse.success(response, "Task updated successfully", HttpStatus.OK.value());
    }

    @GetMapping("/getTask")
    public ApiResponse<Map<String,Object>> getTask(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute GetTaskQueryValidator query
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.getTask(user,query);
        return ApiResponse.success(response, "Task retrieved successfully", HttpStatus.OK.value());
    }

    @GetMapping("/getTasks")
    public ApiResponse<Map<String,Object>> getTasks(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute GetTasksValidator query
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.getTasks(user,query);
        return ApiResponse.success(response, "Tasks retrieved successfully", HttpStatus.OK.value());
    }

}
