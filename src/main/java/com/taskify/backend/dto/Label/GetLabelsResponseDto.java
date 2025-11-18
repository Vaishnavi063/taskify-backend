package com.taskify.backend.dto.Label;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetLabelsResponseDto {
    private List<LabelDto> lables;
    private Integer total;
    private Integer limit;
    private Integer page;
    private Integer totalPages;
    private Integer serialNumberStartFrom;
    private Boolean hasPrevPage;
    private Boolean hasNextPage;
    private Integer prevPage;
    private Integer nextPage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabelDto {
        @JsonProperty("_id")
        private String id;
        private String projectId;
        private String name;
        private String description;
        private String color;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
