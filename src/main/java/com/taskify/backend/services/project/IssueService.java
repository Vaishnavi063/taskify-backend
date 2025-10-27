package com.taskify.backend.services.project;

import com.taskify.backend.repository.project.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;

}

