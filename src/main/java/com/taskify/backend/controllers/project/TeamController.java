package com.taskify.backend.controllers.project;


import com.taskify.backend.services.project.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/project/team")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
}
