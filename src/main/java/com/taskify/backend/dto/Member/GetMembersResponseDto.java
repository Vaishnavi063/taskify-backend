package com.taskify.backend.dto.Member;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetMembersResponseDto {

    private List<MemberDto> members;
    private int total;
    private int limit;
    private int page;
    private int totalPages;
    private int serialNumberStartFrom;
    private boolean hasPrevPage;
    private boolean hasNextPage;
    private Integer prevPage;
    private Integer nextPage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private String _id;
        private String email;
        private String role;
        private InvitationStatus invitationStatus;
        private UserDto user;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String fullName;
    }
}
