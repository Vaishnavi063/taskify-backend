package com.taskify.backend.controllers.project;

import com.taskify.backend.dto.Member.GetMembersResponseDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.MemberService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.GetMembersQuery;
import com.taskify.backend.validators.project.inviteMemberValidator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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

}