package com.taskify.backend.dto.Team;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamDto {
    @JsonProperty("_id")
    private String id;
    private String name;
    private List<MemberDto> members;
    private Object leader;  // Changed to Object to allow both LeaderDto and empty Map
    private Integer memberCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        @JsonProperty("_id")
        private String id;
        private String email;
        private String role;
        private UserDto user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderDto {
        @JsonProperty("_id")
        private String id;
        private String email;
        private String role;
        private UserDto user;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String fullName;
        private Object avatar;
    }
}
