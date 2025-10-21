package com.taskify.backend.controllers.auth;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.auth.UserService;
import com.taskify.backend.validators.auth.RegisterUserRequest;
import com.taskify.backend.utils.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        Map<String, Object> data = userService.registerUser(request);
        return ApiResponse.success(data, "User registered successfully", HttpStatus.CREATED.value());
    }

}
