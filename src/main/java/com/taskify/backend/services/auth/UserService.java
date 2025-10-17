package com.taskify.backend.services.auth;

import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.auth.RegisterUserRequest;
import com.taskify.backend.models.auth.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User registerUser(RegisterUserRequest request) {
        if (userRepository.findByEmail(request.getEmail()) != null) {
            throw new ApiException("Email already in use", HttpStatus.BAD_REQUEST.value());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .build();

        return userRepository.save(user);
    }
}
