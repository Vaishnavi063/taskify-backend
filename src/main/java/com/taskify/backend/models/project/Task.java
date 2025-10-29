package com.taskify.backend.models.project;

import com.taskify.backend.constants.TaskEnums.TaskPriority;
import com.taskify.backend.constants.TaskEnums.TaskStatus;
import com.taskify.backend.models.auth.User;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String memberId;

    private String projectId;

    private String userId;

    private String title;
    private String description;

    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Builder.Default
    private TaskPriority priority = TaskPriority.LOW;

    private LocalDate dueDate;
    private LocalDate completedDate;

    @Builder.Default
    private List<String> assignees = new ArrayList<>();

    @Builder.Default
    private List<String> comments = new ArrayList<>();

    @Builder.Default
    private List<String> subTasks = new ArrayList<>();

    private String taskType;
    private Integer taskNumber;

    @Builder.Default
    private Boolean isDeleted = false;
}
