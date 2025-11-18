package com.taskify.backend.controllers.project;

import com.taskify.backend.dto.Label.GetLabelsResponseDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.project.LabelService;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.project.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/project/label")
@RequiredArgsConstructor
public class LabelController {
    private final LabelService labelService;

    @GetMapping
    public ApiResponse<GetLabelsResponseDto> getLabels(
            HttpServletRequest request,
            @Valid @ModelAttribute GetLabelsQueryValidator query) {
        User user = (User) request.getAttribute("user");
        GetLabelsResponseDto labels = labelService.getLabels(user, query);
        return ApiResponse.success(labels, "Lables fetched successfully", HttpStatus.OK.value());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createLabel(
            HttpServletRequest request,
            @Valid @RequestBody LabelValidator labelRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = labelService.createLabel(user, labelRequest);
        return ApiResponse.success(result, "Lable created successfully", HttpStatus.CREATED.value());
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> updateLabel(
            HttpServletRequest request,
            @Valid @RequestBody UpdateLabelValidator labelRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = labelService.updateLabel(user, labelRequest);
        return ApiResponse.success(result, "Lable updated successfully", HttpStatus.CREATED.value());
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> deleteLabel(
            HttpServletRequest request,
            @Valid @RequestBody DeleteLabelValidator labelRequest) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> result = labelService.deleteLabel(user, labelRequest);
        return ApiResponse.success(result, "Lable deleted successfully", HttpStatus.CREATED.value());
    }
}
