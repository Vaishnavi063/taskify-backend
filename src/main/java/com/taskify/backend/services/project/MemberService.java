package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.dto.Member.GetMembersResponseDto;
import com.taskify.backend.models.auth.PricingModel;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.services.shared.TokenService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.GetMembersQuery;
import com.taskify.backend.validators.project.inviteMemberValidator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final TokenService tokenService;
    private final NotificationService notificationService;

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

    public Map<String,Object> inviteMember(User user, inviteMemberValidator  request) {
        String email = request.getEmail();
        String projectId = request.getProjectId();

        log.info("Inviting member for user {}", user);
        log.info("Inviting member request {}", request);

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 400);
        }
        Project project = projectOpt.get();

        Optional<Member> existingMemberOpt = memberRepository.findByUserIdAndProjectId(email, projectId);
        if (existingMemberOpt.isPresent() &&
                existingMemberOpt.get().getInvitationStatus() == InvitationStatus.ACCEPTED) {
            throw new ApiException(email + " is already a member of this project", 400);
        }

        int count = memberRepository.findByProjectId(projectId).size();

        if (user.getPricingModel() == PricingModel.FREE && count >= 5) {
            throw new ApiException("You can only invite 5 members. Please upgrade your plan.", 400);
        } else if (user.getPricingModel() == PricingModel.PREMIUM && count >= 30) {
            throw new ApiException("You can only invite 30 members. Please upgrade your plan.", 400);
        } else if (user.getPricingModel() == PricingModel.ENTERPRISE && count >= 150) {
            throw new ApiException("You can only invite 150 members. Please contact the support team.", 400);
        }

        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new ApiException("You are not allowed to invite yourself", 400);
        }

        Member member = existingMemberOpt.orElseGet(() -> {
            Member newMember = new Member();
            newMember.setEmail(email);
            newMember.setProjectId(project);
            newMember.setRole(MemberRole.MEMBER);
            newMember.setInvitationStatus(InvitationStatus.PENDING);
            newMember.setCreatedAt(Instant.now());
            newMember.setUpdatedAt(Instant.now());
            return memberRepository.save(newMember);
        });


        long EXP = 60 * 60 * 1000 * 24 * 7;

        Map<String, Object> userPayload = Map.of(
                "email", email,
                "projectId", project.getId(),
                "memberId", member.getId()
        );

        Map<String, Object> payload = Map.of("user", userPayload);

        String inviteToken = tokenService.signToken(payload, EXP);

        String invitationLink = "https://frontend-url.com/invite?token=" + inviteToken +
                "&projectName=" + project.getName() +
                "&email=" + email;


        log.info("Invitation link generated: {}", invitationLink);

        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("name", email);
        templateVariables.put("link", invitationLink);
        templateVariables.put("inviteSenderName", user.getFullName());
        templateVariables.put("projectName", project.getName());

        try {
            notificationService.sendWithTemplate(
                    request.getEmail(),
                    "Invitation from " + project.getName(),
                    "invite-member",
                    templateVariables
            );
        } catch (MessagingException e) {
            throw new ApiException("Failed to send invite email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }


        return Map.of(
                "memberId", member.getId()
        );
    }
}
