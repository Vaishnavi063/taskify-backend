package com.taskify.backend.dto.Task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponseDTO {
    private String id;
    private String projectId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDate dueDate;
    private List<String> tags;
    private LocalDate completedDate;
    private List<Map<String, Object>> subTasks;
    private String taskType;
    private Integer taskNumber;
    private Boolean isMember;
    private Boolean isDeleted;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private CreatorDTO creator;
    private List<MemberDetailDTO> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatorDTO {
        private String memberId;
        private String email;
        private String role;
        private String fullName;
        private Map<String, Object> avatar;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberDetailDTO {
        private String id;
        private String email;
    }
}