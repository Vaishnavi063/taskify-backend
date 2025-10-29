package com.taskify.backend.models.project;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "teams")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Team {

    @Id
    private String id;

    private String projectId;

    private String name;

    @Builder.Default
    private List<String> members = new ArrayList<>();

    private String leader;

    @Builder.Default
    private boolean deleted = false;
}