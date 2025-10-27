package com.taskify.backend.controllers.auth;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.services.auth.UserService;
import com.taskify.backend.validators.auth.*;
import com.taskify.backend.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
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
        return ApiResponse.success(data,"Your password has been reset successfully", HttpStatus.OK.value());
    }

    @PostMapping("/self")
    public ApiResponse<Map<String,Object>> self(@Valid @RequestBody TokenValidator request){
        Map<String,Object> data = userService.self(request);
        return ApiResponse.success(data,"Fetched users details successfully", HttpStatus.OK.value());
    }

    @PatchMapping("/updateFullName")
    public ApiResponse<Map<String, Object>> updateFullName(
            HttpServletRequest request,
            @Valid @RequestBody FullNameValidator body
    ) {
        User user = (User) request.getAttribute("user");
        Map<String, Object> data = userService.updateFullName(user, body);
        return ApiResponse.success(data, "FullName updated successfully", HttpStatus.OK.value());
    }

}
