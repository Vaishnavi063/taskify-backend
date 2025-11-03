package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.constants.MemberEnums.InvitationStatus;
import com.taskify.backend.dto.Member.GetMembersResponseDto;
import com.taskify.backend.models.auth.PricingModel;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.services.shared.NotificationService;
import com.taskify.backend.services.shared.TokenService;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.*;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final NotificationService notificationService;

    @Value("${frontend.url}")
    private String frontendUrl;

    public GetMembersResponseDto getMembers(User user, GetMembersQuery query) {
        log.info("Getting members for user {}", user);
        log.info("Getting members for query {}", query);

        Optional<Project> projectOpt = projectRepository.findById(query.getProjectId());
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 400);
        }

        Optional<Member> memberOpt = memberRepository.findByUserIdAndProjectId(
                user.getId(),
                query.getProjectId()
        );

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
                .filter(m -> email.isEmpty() || Pattern.compile(email, Pattern.CASE_INSENSITIVE)
                        .matcher(m.getEmail()).find())
                .filter(m -> status == null || m.getInvitationStatus().equals(status))
                .collect(Collectors.toList());

        int page = Math.max(0, query.getPage() - 1);
        int limit = query.getLimit();
        int totalMembers = filteredMembers.size();
        int totalPages = (int) Math.ceil((double) totalMembers / limit);

        int start = page * limit;
        int end = Math.min(start + limit, totalMembers);
        List<Member> paginated = filteredMembers.subList(start, end);

        List<GetMembersResponseDto.MemberDto> memberDtos = paginated.stream().map(m -> {
            String fullName = "Pending";

            if (m.getUserId() != null && !m.getUserId().isEmpty()) {
                Optional<User> userOpt = userRepository.findById(m.getUserId());
                if (userOpt.isPresent()) {
                    fullName = userOpt.get().getFullName();
                }
            }

            GetMembersResponseDto.UserDto userDto = new GetMembersResponseDto.UserDto(fullName);

            return new GetMembersResponseDto.MemberDto(
                    m.getId(),
                    m.getEmail(),
                    m.getRole() != null ? m.getRole().toString() : "N/A",
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

    public Map<String, Object> inviteMember(User user, inviteMemberValidator request) {
        String email = request.getEmail();
        String projectId = request.getProjectId();

        log.info("Inviting member for user {}", user);
        log.info("Inviting member request {}", request);
        log.info("Inviting member request email {}", email);

        Optional<Project> projectOpt = projectRepository.findById(projectId);
        if (projectOpt.isEmpty()) {
            throw new ApiException("Project not found", 400);
        }
        Project project = projectOpt.get();

        Optional<Member> existingMemberOpt = memberRepository.findByEmailAndProjectId(email, projectId);
        log.info("Existing member found: {}", existingMemberOpt.isPresent() ? existingMemberOpt.get() : "none");

        if (existingMemberOpt.isPresent()) {
            InvitationStatus status = existingMemberOpt.get().getInvitationStatus();
            if (status == InvitationStatus.ACCEPTED) {
                throw new ApiException(email + " is already a member of this project", 400);
            } else if (status == InvitationStatus.PENDING) {
                throw new ApiException(email + " has already been invited", 400);
            }
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
            newMember.setProjectId(projectId);  // Store only project ID
            newMember.setRole(MemberRole.MEMBER);
            newMember.setInvitationStatus(InvitationStatus.PENDING);
            newMember.setCreatedAt(Instant.now());
            newMember.setUpdatedAt(Instant.now());
            return memberRepository.save(newMember);
        });

        long EXP = 60 * 60 * 1000 * 24 * 7;  // 7 days

        Map<String, Object> userPayload = Map.of(
                "email", email,
                "projectId", project.getId(),
                "memberId", member.getId()
        );

        Map<String, Object> payload = Map.of("user", userPayload);

        String inviteToken = tokenService.signToken(payload, EXP);

        String invitationLink = frontendUrl + "/guidance/invitation?invitationToken=" + inviteToken +
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
            log.error("Failed to send invite email", e);
            throw new ApiException("Failed to send invite email", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        return Map.of("memberId", member.getId());
    }

    public Map<String, Object> changeInvitationStatus(User user, changeInvitationStatusValidator request) {
        String userEmail = user.getEmail();
        InvitationStatus invitationStatus = request.getInvitationStatus();
        String invitationToken = request.getInvitationToken();

        log.info("Changing invitation status for user {}", userEmail);
        log.info("Changing invitation status request {}", request);

        Map<String, Object> decodedMember = tokenService.verifyToken(invitationToken);
        if (decodedMember == null || !decodedMember.containsKey("user")) {
            throw new ApiException("Invitation token is invalid", 400);
        }

        log.info("Decoded member: {}", decodedMember);

        // ✅ Check token expiration
        Object expObj = decodedMember.get("exp");
        log.info("Invitation exp obj {}", expObj);
        if (expObj != null && ((Number) expObj).longValue() * 1000 <= System.currentTimeMillis()) {
            throw new ApiException("Invitation token is expired", 400);
        }

        Map<String, Object> tokenUser = (Map<String, Object>) decodedMember.get("user");
        if (tokenUser.containsKey("user")) {
            Object nestedUser = tokenUser.get("user");
            if (nestedUser instanceof Map<?, ?>) {
                tokenUser = (Map<String, Object>) nestedUser;
            } else {
                throw new ApiException("Invalid token payload: nested user is malformed", 400);
            }
        }

        log.info("Changing invitation status for token user {}", tokenUser);

        String tokenEmail = tokenUser.get("email") != null ? tokenUser.get("email").toString().trim() : "";
        String userEmailSafe = userEmail != null ? userEmail.trim() : "";

        log.info("Check Email - token: {}, current user: {}", tokenEmail, userEmailSafe);

        if (!tokenEmail.equalsIgnoreCase(userEmailSafe)) {
            throw new ApiException(
                    "You are not allowed to " + invitationStatus + " this invitation", 400
            );
        }

        String memberId = (String) tokenUser.get("memberId");
        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }

        Member member = memberOpt.get();

        if (member.getInvitationStatus() == InvitationStatus.ACCEPTED) {
            throw new ApiException("This user has already accepted the project invitation", 400);
        }

        member.setInvitationStatus(invitationStatus);
        member.setEmail(tokenEmail);
        member.setUserId(user.getId());
        member.setUpdatedAt(Instant.now());
        memberRepository.save(member);

        return Map.of(
                "memberId", member.getId(),
                "email", member.getEmail(),
                "invitationStatus", member.getInvitationStatus()
        );
    }

    public Map<String,Object> removeMember(User user, RemoveMemberValidator request) {
        String userId = user.getId();
        String memberId = request.getMemberId();
        String projectId = request.getProjectId();
        log.info("Removing member from user {}", userId);
        log.info("Removing member request {}", memberId, projectId);


        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }

        Member member = memberOpt.get();

        if(!member.getRole().equals(MemberRole.MEMBER)){
            throw new ApiException("You can not remove the owner of the project", 400);
        }

        Optional<Member> ownerOpt = memberRepository.findByUserIdAndProjectId(userId, projectId);
        if (ownerOpt.isEmpty()) {
            throw new ApiException("You have no permission to remove member the project", 403);
        }

        Member owner = ownerOpt.get();
        if(!owner.getRole().equals(MemberRole.OWNER)){
            throw new ApiException("Only the owner can remove a member from the project", 400);
        }

        memberRepository.deleteById(memberId);
        log.info("Member {} removed successfully", memberId);

        return Map.of(
                "memberId", memberId
        );
    }

    public Map<String,Object> updateMember(User user, UpdateMemberValidator request) {
        String memberId = request.getMemberId();
        String projectId = request.getProjectId();
        MemberRole newRole = request.getRole();

        log.info("Updating member request: memberId={}, projectId={}, newRole={}", memberId, projectId, newRole);

        Optional<Member> memberOpt = memberRepository.findById(memberId);
        if (memberOpt.isEmpty()) {
            throw new ApiException("Member not found", 404);
        }

        Member member = memberOpt.get();

        if(member.getRole().equals(MemberRole.OWNER)){
            throw new ApiException("You cannot update the owner of the project", 400);
        }

        Optional<Member> ownerOpt = memberRepository.findByUserIdAndProjectId(user.getId(), projectId);
        if (ownerOpt.isEmpty() || !ownerOpt.get().getRole().equals(MemberRole.OWNER)) {
            throw new ApiException("Only the owner can update a member role", 403);
        }

        member.setRole(newRole);
        memberRepository.save(member);
        log.info("Member {} role updated to {}", memberId, newRole);

        return Map.of(
                "memberId", memberId,
                "newRole", newRole,
                "status", "updated"
        );
    }

}