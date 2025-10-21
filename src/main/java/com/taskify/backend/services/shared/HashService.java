package com.taskify.backend.services.shared;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class HashService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    // Hash plain text data (like password)
    public String hashData(String data) {
        return passwordEncoder.encode(data);
    }

    // Compare plain text with hashed data
    public boolean hashCompare(String data, String hashedData) {
        return passwordEncoder.matches(data, hashedData);
    }
}
