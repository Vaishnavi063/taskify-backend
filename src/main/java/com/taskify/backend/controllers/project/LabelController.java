package com.taskify.backend.controllers.project;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Label;
import com.taskify.backend.services.project.LabelService;
import com.taskify.backend.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/v1/project/label")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;

    @PostMapping("")
    public ApiResponse<Map<String, Object>> create(HttpServletRequest request, @RequestBody Label label) {
        User user = (User) request.getAttribute("user");
        Map<String ,Object> response = labelService.create(user,label);
        return ApiResponse.success(response, "Lable created successfully'", HttpStatus.OK.value());
    }
}
