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

import java.util.*;


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

    @PatchMapping("/changeStatus")
    public ApiResponse<Map<String,Object>> changeStatus(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ChangeStatusValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.changeStatus(user,body);
        return ApiResponse.success(response, "Task status changed successfully", HttpStatus.OK.value());
    }

    @GetMapping("/getMembersCompletedTasks")
    public ApiResponse<List<Map<String, Object>>> getMembersCompletedTasks(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute ValidateProjectIdQuery query
    ) {
        User user = (User) httpRequest.getAttribute("user");
        List<Map<String, Object>> response = taskService.getMembersCompletedTasks(user, query);
        return ApiResponse.success(response, "Get user completed tasks successfully", HttpStatus.OK.value());
    }

    @GetMapping("/getUserAssignedTasks")
    public ApiResponse<Map<String,Object>> getUserAssignedTasks(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute ValidateProjectIdQuery query
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.getUserAssignedTasks(user,query);
        return ApiResponse.success(response, "Get assigned tasks successfully", HttpStatus.OK.value());
    }

    @GetMapping("/getLast30DaysTasks")
    public ApiResponse<List<Map<String, Object>>> getLast30DaysTasks(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute ValidateProjectIdQuery query
    ){
        User user =  (User) httpRequest.getAttribute("user");
        List<Map<String, Object>> response = taskService.getLast30DaysTasks(user,query);
        return ApiResponse.success(response, "Get assigned tasks successfully", HttpStatus.OK.value());
    }

    @PostMapping("/assignMember")
    public ApiResponse<Map<String,Object>> assignMember(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AssignMemberValidator  body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.assignMember(user,body);
        return ApiResponse.success(response, "Assigned member successfully", HttpStatus.OK.value());
    }

    @DeleteMapping("/deleteTask")
    public ApiResponse<Map<String,Object>> deleteTask(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ValidateTaskId query
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.deleteTask(user,query);
        return ApiResponse.success(response, "Deleted task successfully", HttpStatus.OK.value());
    }

    @PostMapping("/removeAssignedMember")
    public ApiResponse<Map<String,Object>> removeAssignedMember(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AssignMemberValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.removeAssignedMember(user, body);
        return ApiResponse.success(response, "Assigned member removed successfully", HttpStatus.OK.value());
    }

    @PostMapping("/addComment")
    public ApiResponse<Map<String,Object>> addComment(
            HttpServletRequest httpRequest,
            @Valid @RequestBody AddCommentValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.addComment(user, body);
        return ApiResponse.success(response, "Comment added successfully", HttpStatus.CREATED.value());
    }

    @DeleteMapping("/removeComment")
    public ApiResponse<Map<String,Object>> removeComment(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RemoveCommentValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.removeComment(user, body);
        return ApiResponse.success(response, "Comment removed successfully", HttpStatus.OK.value());
    }

    @PatchMapping("/updateComment")
    public ApiResponse<Map<String,Object>> updateComment(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateCommentValidator body
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.updateComment(user, body);
        return ApiResponse.success(response, "Comment added successfully", HttpStatus.CREATED.value());
    }

    @GetMapping("/getCompletedTasks")
    public ApiResponse<Map<String,Object>> getCompletedTasks(
            HttpServletRequest httpRequest,
            @Valid @ModelAttribute GetTasksValidator query
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String,Object> response = taskService.getCompletedTasks(user, query);
        return ApiResponse.success(response, "Completed tasks fetched successfully", HttpStatus.OK.value());
    }

}
