package com.taskify.backend.dto.Team;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetTeamsResponseDto {
    private List<TeamDto> teams;
    private Integer total;
    private Integer limit;
    private Integer page;
    private Integer totalPages;
    private Integer serialNumberStartFrom;
    private Boolean hasPrevPage;
    private Boolean hasNextPage;
    private Integer prevPage;
    private Integer nextPage;
}
