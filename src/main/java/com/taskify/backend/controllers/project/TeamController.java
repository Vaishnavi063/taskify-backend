package com.taskify.backend.controllers.project;


import com.taskify.backend.dto.Team.GetTeamsResponseDto;
import com.taskify.backend.dto.Team.TeamDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.TeamService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/project/team")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    @GetMapping
    public ApiResponse<GetTeamsResponseDto> getTeams(
            HttpServletRequest request,
            @Valid @ModelAttribute GetTeamsQueryValidator query) {
        User user = (User) request.getAttribute("user");
        GetTeamsResponseDto teams = teamService.getTeams(user, query);
        return ApiResponse.success(teams, "Teams fetched successfully", HttpStatus.OK.value());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createTeam(
            HttpServletRequest request,
            @Valid @RequestBody TeamValidator teamRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = teamService.createTeam(user, teamRequest);
        return ApiResponse.success(result, "Team created successfully", HttpStatus.OK.value());
    }

    @PatchMapping
    public ApiResponse<Map<String, Object>> updateTeam(
            HttpServletRequest request,
            @Valid @RequestBody UpdateTeamValidator teamRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = teamService.updateTeam(user, teamRequest);
        return ApiResponse.success(result, "Team updated successfully", HttpStatus.OK.value());
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> deleteTeam(
            HttpServletRequest request,
            @Valid @RequestBody DeleteTeamValidator teamRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = teamService.deleteTeam(user, teamRequest);
        return ApiResponse.success(result, "Team deleted successfully", HttpStatus.OK.value());
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> addOrRemoveTeamMember(
            HttpServletRequest request,
            @Valid @RequestBody AddOrRemoveTeamMemberValidator teamRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = teamService.addOrRemoveTeamMember(user, teamRequest);
        String message = Boolean.TRUE.equals(teamRequest.getIsRemove()) 
                ? "Team member removed successfully" 
                : "Team member added successfully";
        return ApiResponse.success(result, message, HttpStatus.OK.value());
    }

    @GetMapping("/getTeam")
    public ApiResponse<TeamDto> getTeam(
            HttpServletRequest request,
            @Valid @ModelAttribute GetTeamQueryValidator query) {
        User user = (User) request.getAttribute("user");
        TeamDto team = teamService.getTeam(user, query);
        return ApiResponse.success(team, "Team fetched successfully", HttpStatus.OK.value());
    }

    @PutMapping("/leader")
    public ApiResponse<Map<String, Object>> addOrRemoveTeamLeader(
            HttpServletRequest request,
            @Valid @RequestBody AddOrRemoveTeamLeaderValidator teamRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = teamService.addOrRemoveTeamLeader(user, teamRequest);
        String message = Boolean.TRUE.equals(teamRequest.getIsRemove()) 
                ? "Team leader removed successfully" 
                : "Team leader added successfully";
        return ApiResponse.success(result, message, HttpStatus.OK.value());
    }
}
