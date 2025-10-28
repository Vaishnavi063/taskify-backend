package com.taskify.backend.models.project;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    private String id;

    private String content;

    @DBRef
    private Member memberId;

    @Builder.Default
    private int likes = 0;

    @Builder.Default
    private List<String> replies = new ArrayList<>();

    @Builder.Default
    private String commentType = "General";

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}