package com.taskify.backend.controllers.project;

import com.taskify.backend.dto.Member.GetMembersResponseDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.MemberService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/project/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/getMembers")
    public ApiResponse<GetMembersResponseDto> getMembers(
            HttpServletRequest request,
            @ModelAttribute GetMembersQuery query
    ) {
        User user = (User) request.getAttribute("user");
        query.sanitize();

        GetMembersResponseDto response = memberService.getMembers(user, query);
        return ApiResponse.success(response, "Members fetched successfully", HttpStatus.OK.value());
    }

    @PostMapping("/inviteMember")
    public ApiResponse<Map<String, Object>> inviteMember(
            HttpServletRequest httpRequest,
            @Valid @RequestBody inviteMemberValidator request
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = memberService.inviteMember(user,request);
        return ApiResponse.success(response, "Invite member successfully", HttpStatus.OK.value());
    }

    @PatchMapping("/changeInvitationStatus")
    public ApiResponse<Map<String, Object>> changeInvitationStatus(
            HttpServletRequest httpRequest,
            @Valid @RequestBody changeInvitationStatusValidator request
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = memberService.changeInvitationStatus(user,request);
        return ApiResponse.success(response, "Invite member successfully", HttpStatus.OK.value());
    }

    @DeleteMapping("/removeMember")
    public ApiResponse<Map<String, Object>> removeMember(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RemoveMemberValidator request
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = memberService.removeMember(user,request);
        return ApiResponse.success(response, "Member removed successfully", HttpStatus.OK.value());
    }

    @PatchMapping("/updateMember")
    public ApiResponse<Map<String, Object>> updateMember(
            HttpServletRequest httpRequest,
            @Valid @RequestBody UpdateMemberValidator request
    ){
        User user = (User) httpRequest.getAttribute("user");
        Map<String, Object> response = memberService.updateMember(user,request);
        return ApiResponse.success(response, "Member updated successfully", HttpStatus.OK.value());
    }

}