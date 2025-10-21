package com.taskify.backend.services.auth;

import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.services.shared.TokenService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.auth.RegisterUserRequest;
import com.taskify.backend.models.auth.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final NotificationService notificationService;
    @Value("${frontend.url}")
    private String frontendUrl;

    public Map<String, Object> registerUser(RegisterUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new ApiException("Email already in use", HttpStatus.BAD_REQUEST.value());
        }

        long EXP = 10 * 60 * 1000;

        String verifyEmailToken = tokenService.signToken(Map.of(
                "fullName", request.getFullName(),
                "email", request.getEmail()
        ), EXP);

        String verificationLink = frontendUrl + "/verify?token=" + verifyEmailToken;

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("fullName", request.getFullName());
        templateVariables.put("verificationLink", verificationLink);

        try {
            notificationService.sendWithTemplate(
                    request.getEmail(),
                    "Verify Email and Create Password",
                    "verify-email",
                    templateVariables
            );
        } catch (MessagingException e) {
            throw new ApiException("Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("email", request.getEmail());
        data.put("fullName", request.getFullName());


        return data;
    }

}
