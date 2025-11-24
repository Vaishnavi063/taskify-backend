package com.taskify.backend.services.auth;

import com.taskify.backend.models.auth.PricingModel;
import com.taskify.backend.models.auth.UserRoles;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.services.shared.HashService;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.services.shared.TokenService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.auth.*;
import com.taskify.backend.models.auth.User;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

        String verificationLink = frontendUrl + "/auth/create-password?token=" + verifyEmailToken;

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

        Object expObj = decodedToken.get("exp");
        if (expObj == null) {
            throw new ApiException("Token expiration missing.", HttpStatus.BAD_REQUEST.value());
        }

        long expMillis = ((Number) expObj).longValue() * 1000;
        if (expMillis < System.currentTimeMillis()) {
            throw new ApiException("Token has expired. Please request a new one.", HttpStatus.BAD_REQUEST.value());
        }

        Map<String, Object> userMap = (Map<String, Object>) decodedToken.get("user");
        if (userMap.containsKey("user")) {
            Object nestedUser = userMap.get("user");
            if (nestedUser instanceof Map<?, ?>) {
                userMap = (Map<String, Object>) nestedUser;
            } else {
                throw new ApiException("Invalid token payload: nested user is malformed.", HttpStatus.BAD_REQUEST.value());
            }
        }

        String email = (String) userMap.get("email");
        String fullName = (String) userMap.get("fullName");

        if (email == null || fullName == null) {
            throw new ApiException("Invalid token payload: email or fullName missing.", HttpStatus.BAD_REQUEST.value());
        }

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

        long EXP = 1000L * 60 * 60 * 24 * 30; // 30 days
        String accessToken = tokenService.signToken(
                Map.of("user", Map.of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName()
                )), EXP
        );

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

    public Map<String, Object> login(UserLoginValidator request) {
        String email = request.getEmail();
        String password = request.getPassword();

        log.info("Login request {}", request);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ApiException("User not found.", HttpStatus.BAD_REQUEST.value());
        }

        boolean isMatch = hashService.hashCompare(password, user.getPassword());

        if (!isMatch) {
            throw new ApiException("Passwords don't match.", HttpStatus.BAD_REQUEST.value());
        }

        long EXP = 1000L * 60 * 60 * 24 * 30; // 30 days
        String accessToken = tokenService.signToken(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "password", user.getPassword()
        ), EXP);


        Map<String, Object> userMap = new HashMap<>();
        userMap.put("_id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", Optional.ofNullable(user.getFullName()).orElse(""));
        userMap.put("role", Optional.ofNullable(user.getRole()).map(UserRoles::name).orElse(""));
        userMap.put("avatar", Optional.ofNullable(user.getAvatar()).orElse(""));
        userMap.put("pricingModel", Optional.ofNullable(user.getPricingModel()).map(PricingModel::name).orElse(""));


        return Map.of(
                "user", userMap,
                "token", accessToken
        );
    }

    public Map<String,Object> forgotPassoword(UserEmailValidator request) {
        String email = request.getEmail();
        log.info("Forgot password request {}", request);

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ApiException("User not found.", HttpStatus.BAD_REQUEST.value());
        }

        long EXP = 1000 * 60 * 60 * 24 * 2;

        String resetPasswordToken = tokenService.signToken(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "fullName", user.getFullName()
        ),EXP);

        String resetPasswordLink = frontendUrl + "/auth/reset-password?token=" + resetPasswordToken;

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("fullName", user.getFullName());
        templateVariables.put("resetPasswordLink", resetPasswordLink);

        try {
            notificationService.sendWithTemplate(
                    request.getEmail(),
                    "Reset Your Password",
                    "reset-password",
                    templateVariables
            );
        } catch (MessagingException e) {
            throw new ApiException("Failed to send verification email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("email", request.getEmail());

        return data;
    }

    public Map<String, Object> resetPassword(VerifyEmailAndCreatePasswordRequest request) {
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();
        String token = request.getToken();

        log.info("Reset password request {}", request);

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

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ApiException("User not found. Please sign up first.", HttpStatus.BAD_REQUEST.value());
        }

        if (!password.equals(confirmPassword)) {
            throw new ApiException("Passwords don't match.", HttpStatus.BAD_REQUEST.value());
        }

        String hashedPassword = hashService.hashData(password);
        user.setPassword(hashedPassword);
        userRepository.save(user);

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

    public Map<String, Object> self(TokenValidator request) {
        String token = request.getToken();
        log.info("Self request {}", request);

        Map<String, Object> decodedToken = tokenService.verifyToken(token);

        if (decodedToken == null || !decodedToken.containsKey("user")) {
            throw new ApiException("The token provided is invalid or expired.", HttpStatus.BAD_REQUEST.value());
        }

        Map<String, Object> userMap = (Map<String, Object>) decodedToken.get("user");
        String email = (String) userMap.get("email");

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ApiException("User not found. Please sign up first.", HttpStatus.BAD_REQUEST.value());
        }

        return Map.of(
                "user", userMap,
                "token", token
        );
    }

    public Map<String, Object> updateFullName(User user, FullNameValidator request) {
        String fullName = request.getFullName();
        log.info("Update full name request {}", request);
        log.info("User {}", user);

        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        existingUser.setFullName(fullName);
        userRepository.save(existingUser);

        Map<String, Object> userData = Map.of(
                "userId", existingUser.getId(),
                "fullName", existingUser.getFullName()
        );

        return Map.of("user", userData);
    }

}
