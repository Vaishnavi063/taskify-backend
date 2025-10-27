package com.taskify.backend.controllers.project;

import com.taskify.backend.services.project.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/project/issue")
@RequiredArgsConstructor
public class IssueController {
    private final IssueService issueService;
}
