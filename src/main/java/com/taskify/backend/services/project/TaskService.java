package com.taskify.backend.services.project;

import com.taskify.backend.repository.project.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
}
