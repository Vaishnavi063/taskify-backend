package com.taskify.backend.controllers.auth;

import com.taskify.backend.services.auth.UserService;
import com.taskify.backend.validators.auth.RegisterUserRequest;
import com.taskify.backend.utils.ApiResponse;
import com.taskify.backend.validators.auth.UserEmailValidator;
import com.taskify.backend.validators.auth.UserLoginValidator;
import com.taskify.backend.validators.auth.VerifyEmailAndCreatePasswordRequest;
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

    @PostMapping("/verifyEmailAndCreatePassword")
    public ApiResponse<Map<String, Object>> verifyEmailAndCreatePassword(@Valid @RequestBody VerifyEmailAndCreatePasswordRequest request) {
        Map<String, Object> data = userService.verifyEmailAndCreatePassword(request);
        return ApiResponse.success(data, "User registered successfully", HttpStatus.CREATED.value());
    }

    @PostMapping("/login")
    public ApiResponse<Map<String,Object>> login(@Valid @RequestBody UserLoginValidator request){
        Map<String, Object> data = userService.login(request);
        return ApiResponse.success(data, "User logged successfully", HttpStatus.OK.value());
    }

    @PostMapping("/forgotPassword")
    public ApiResponse<Map<String,Object>> forgotPassword(@Valid @RequestBody UserEmailValidator request){
        Map<String,Object> data = userService.forgotPassoword(request);
        return ApiResponse.success(data, "Reset password link has been sent to your email", HttpStatus.OK.value());
    }

    @PostMapping("/resetPassword")
    public ApiResponse<Map<String,Object>> resetPassword(@Valid @RequestBody VerifyEmailAndCreatePasswordRequest request){
        Map<String,Object> data = userService.resetPassword(request);
        return ApiResponse .success(data,"Your password has been reset successfully", HttpStatus.OK.value());
    }
}
