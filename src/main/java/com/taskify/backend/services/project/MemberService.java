package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.dto.Member.GetMembersResponseDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.GetMembersQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;

    public GetMembersResponseDto getMembers(User user, GetMembersQuery query) {
        log.info("Getting members for user {}", user);
        log.info("Getting members for query {}", query);

        Optional<Project> projectOpt = projectRepository.findById(query.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 400);
        }

        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(user.getId(), query.getProjectId());
        System.out.println("MEMBERS: " + memberOpt);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 400);
        }

        Member member = memberOpt.get();
        if (!member.getInvitationStatus().equals(InvitationStatus.ACCEPTED)) {
            throw new ApiException("You have not accepted the invitation of this project", 403);
        }

        return getMembersByProjectId(query);
    }

    private GetMembersResponseDto getMembersByProjectId(GetMembersQuery query) {
        final String email = query.getEmail() != null ? query.getEmail() : "";
        final InvitationStatus status = Optional.ofNullable(query.getInvitationStatus())
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return InvitationStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .orElse(null);

        List<Member> allMembers = memberRepository.findByProjectId(query.getProjectId());

        List<Member> filteredMembers = allMembers.stream()
                .filter(m -> email.isEmpty() || Pattern.compile(email, Pattern.CASE_INSENSITIVE).matcher(m.getEmail()).find())
                .filter(m -> status == null || m.getInvitationStatus().equals(status))
                .collect(Collectors.toList());

        int page = Math.max(0, query.getPage() - 1); // zero-based page number
        int limit = query.getLimit();
        int totalMembers = filteredMembers.size();
        int totalPages = (int) Math.ceil((double) totalMembers / limit);

        int start = page * limit;
        int end = Math.min(start + limit, totalMembers);
        List<Member> paginated = filteredMembers.subList(start, end);

        List<GetMembersResponseDto.MemberDto> memberDtos = paginated.stream().map(m -> {
            GetMembersResponseDto.UserDto userDto = new GetMembersResponseDto.UserDto(
                    m.getUserId().getFullName()
            );

            return new GetMembersResponseDto.MemberDto(
                    m.getId(),
                    m.getEmail(),
                    m.getRole().toString(),
                    m.getInvitationStatus(),
                    userDto
            );
        }).collect(Collectors.toList());

        GetMembersResponseDto responseDto = new GetMembersResponseDto();
        responseDto.setMembers(memberDtos);
        responseDto.setTotal(totalMembers);
        responseDto.setLimit(limit);
        responseDto.setPage(query.getPage());
        responseDto.setTotalPages(totalPages);
        responseDto.setSerialNumberStartFrom((page * limit) + 1);
        responseDto.setHasPrevPage(page > 0);
        responseDto.setHasNextPage(page < totalPages - 1);
        responseDto.setPrevPage(page > 0 ? query.getPage() - 1 : null);
        responseDto.setNextPage(page < totalPages - 1 ? query.getPage() + 1 : null);

        return responseDto;
    }
}
