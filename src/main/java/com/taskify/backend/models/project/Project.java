package com.taskify.backend.models.project;

import com.taskify.backend.models.auth.User;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projects")
public class Project {

    @Id
    private String id;

    private String name;

    private String description;

    private List<String> tags = new ArrayList<>();

    private String userId;

    private boolean isDeleted = false;

}

