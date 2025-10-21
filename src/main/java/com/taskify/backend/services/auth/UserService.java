package com.taskify.backend.services.auth;

import com.taskify.backend.models.auth.UserRoles;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.services.shared.HashService;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.services.shared.TokenService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.auth.RegisterUserRequest;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.validators.auth.VerifyEmailAndCreatePasswordRequest;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final HashService hashService;
    private final NotificationService notificationService;
    @Value("${frontend.url}")
    private String frontendUrl;

    public Map<String, Object> registerUser(RegisterUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new ApiException("Email already in use", HttpStatus.BAD_REQUEST.value());
        }

        log.info("Registering user {}", request);

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

    public Map<String, Object> verifyEmailAndCreatePassword(VerifyEmailAndCreatePasswordRequest request) {
        String token = request.getToken();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        log.info("Verifying email and create password request {}", request);

        Map<String, Object> decodedToken = tokenService.verifyToken(token);

        if (decodedToken == null || !decodedToken.containsKey("user")) {
            throw new ApiException("The token provided is invalid or expired.", HttpStatus.BAD_REQUEST.value());
        }

        long expMillis = ((Number) decodedToken.get("exp")).longValue() * 1000;
        if (expMillis < System.currentTimeMillis()) {
            throw new ApiException("Token has expired. Please request a new one.", HttpStatus.BAD_REQUEST.value());
        }

        Map<String, Object> userMap = (Map<String, Object>) decodedToken.get("user");
        String email = (String) userMap.get("email");
        String fullName = (String) userMap.get("fullName");

        if (userRepository.findByEmail(email) != null) {
            throw new ApiException("User with this email already exists.", HttpStatus.CONFLICT.value());
        }

        if (!password.equals(confirmPassword)) {
            throw new ApiException("Passwords don't match.", HttpStatus.BAD_REQUEST.value());
        }

        String hashedPassword = hashService.hashData(password);

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .password(hashedPassword)
                .role(UserRoles.USER)
                .build();

        user = userRepository.save(user);

        long EXP = 1000 * 60 * 60 * 24 * 30;
        String accessToken = tokenService.signToken(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName()
        ), EXP);

        return Map.of(
                "user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "role", user.getRole()
                ),
                "token", accessToken
        );
    }

}
