package com.taskify.backend.controllers.project;

import com.taskify.backend.services.project.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/project/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
}
