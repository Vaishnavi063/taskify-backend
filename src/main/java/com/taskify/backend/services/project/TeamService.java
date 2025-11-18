package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.dto.Team.GetTeamsResponseDto;
import com.taskify.backend.dto.Team.TeamDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Project;
import com.taskify.backend.models.project.Team;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.repository.project.ProjectRepository;
import com.taskify.backend.repository.project.TeamRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    public GetTeamsResponseDto getTeams(User user, GetTeamsQueryValidator query) {
        String userId = user.getId();
        String projectId = query.getProjectId();

        log.info("Getting teams - userId: {}, projectId: {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        return getTeamsWithMembers(query);
    }

    private GetTeamsResponseDto getTeamsWithMembers(GetTeamsQueryValidator query) {
        String projectId = query.getProjectId();
        String name = query.getName() != null ? query.getName() : "";
        int page = Math.max(0, query.getPage() - 1);
        int limit = query.getLimit();

        // Use simple repository query instead of aggregation
        List<Team> allTeams = teamRepository.findAll().stream()
                .filter(team -> team.getProjectId().equals(projectId))
                .filter(team -> name.isEmpty() || team.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());

        // Map to DTOs with member and leader details
        List<TeamDto> teams = allTeams.stream()
                .map(this::mapTeamToDto)
                .collect(Collectors.toList());

        int total = teams.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int start = page * limit;
        int end = Math.min(start + limit, total);
        List<TeamDto> paginatedTeams = start < total ? teams.subList(start, end) : new ArrayList<>();

        return GetTeamsResponseDto.builder()
                .teams(paginatedTeams)
                .total(total)
                .limit(limit)
                .page(query.getPage())
                .totalPages(totalPages)
                .serialNumberStartFrom((page * limit) + 1)
                .hasPrevPage(page > 0)
                .hasNextPage(page < totalPages - 1)
                .prevPage(page > 0 ? query.getPage() - 1 : null)
                .nextPage(page < totalPages - 1 ? query.getPage() + 1 : null)
                .build();
    }

    private TeamDto mapTeamToDto(Team team) {
        TeamDto.TeamDtoBuilder builder = TeamDto.builder()
                .id(team.getId())
                .name(team.getName());

        // Get leader details
        if (team.getLeader() != null) {
            Member leader = memberRepository.findById(team.getLeader()).orElse(null);
            if (leader != null) {
                User leaderUser = userRepository.findById(leader.getUserId()).orElse(null);
                builder.leader(TeamDto.LeaderDto.builder()
                        .id(leader.getId())
                        .email(leader.getEmail())
                        .role(leader.getRole().toString())
                        .user(leaderUser != null ? TeamDto.UserDto.builder()
                                .fullName(leaderUser.getFullName())
                                .avatar(leaderUser.getAvatar())
                                .build() : null)
                        .build());
            } else {
                builder.leader(new HashMap<>());  // Empty object if leader not found
            }
        } else {
            builder.leader(new HashMap<>());  // Empty object if no leader
        }

        // Get member details
        List<TeamDto.MemberDto> memberDtos = team.getMembers().stream()
                .map(memberId -> {
                    Member m = memberRepository.findById(memberId).orElse(null);
                    if (m == null) return null;
                    User u = userRepository.findById(m.getUserId()).orElse(null);
                    return TeamDto.MemberDto.builder()
                            .id(m.getId())
                            .email(m.getEmail())
                            .role(m.getRole().toString())
                            .user(u != null ? TeamDto.UserDto.builder()
                                    .fullName(u.getFullName())
                                    .avatar(u.getAvatar())
                                    .build() : null)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        builder.members(memberDtos);

        return builder.build();
    }

    public Map<String, Object> createTeam(User user, TeamValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String name = request.getName();

        log.info("Creating team - userId: {}, projectId: {}, name: {}", userId, projectId, name);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permissions for create team", HttpStatus.FORBIDDEN.value());
        }

        Optional<Team> existingTeam = teamRepository.findByNameAndProjectId(name, projectId);
        if (existingTeam.isPresent()) {
            throw new ApiException("Team name already exists", HttpStatus.BAD_REQUEST.value());
        }

        Team team = Team.builder()
                .projectId(projectId)
                .name(name)
                .members(new ArrayList<>())
                .build();

        team = teamRepository.save(team);

        // Return formatted DTO instead of raw entity
        TeamDto teamDto = TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .members(new ArrayList<>())
                .leader(new HashMap<>())  // Empty object instead of null
                .memberCount(0)
                .build();

        return Map.of("team", teamDto);
    }

    public Map<String, Object> updateTeam(User user, UpdateTeamValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String teamId = request.getTeamId();
        String name = request.getName();

        log.info("Updating team - userId: {}, teamId: {}", userId, teamId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permissions for update team", HttpStatus.FORBIDDEN.value());
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND.value()));

        team.setName(name);
        Team updatedTeam = teamRepository.save(team);

        // Return formatted DTO
        TeamDto teamDto = mapTeamToDto(updatedTeam);

        return Map.of("team", teamDto);
    }

    public Map<String, Object> deleteTeam(User user, DeleteTeamValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String teamId = request.getTeamId();

        log.info("Deleting team - userId: {}, teamId: {}", userId, teamId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permissions for delete team", HttpStatus.FORBIDDEN.value());
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND.value()));

        teamRepository.deleteById(teamId);

        return Map.of("teamId", teamId);
    }

    public Map<String, Object> addOrRemoveTeamMember(User user, AddOrRemoveTeamMemberValidator request) {
        String userId = user.getId();
        String teamId = request.getTeamId();
        String memberId = request.getMemberId();
        String projectId = request.getProjectId();
        Boolean isRemove = request.getIsRemove();

        log.info("Add/Remove team member - userId: {}, teamId: {}, memberId: {}, isRemove: {}", 
                userId, teamId, memberId, isRemove);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND.value()));

        Member memberToAdd = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        Member userMember = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("You have no permission to add member", HttpStatus.FORBIDDEN.value()));

        if (userMember.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to add member", HttpStatus.FORBIDDEN.value());
        }

        if (isRemove) {
            if (!team.getMembers().contains(memberId)) {
                throw new ApiException("Member not exists in the team", HttpStatus.CONFLICT.value());
            }
            team.getMembers().remove(memberId);
        } else {
            if (team.getMembers().contains(memberId)) {
                throw new ApiException("Member already exists in the team", HttpStatus.CONFLICT.value());
            }
            team.getMembers().add(memberId);
        }

        teamRepository.save(team);

        Map<String, Object> response = new HashMap<>();
        response.put("teamId", teamId);
        if (isRemove) {
            response.put("memberId", memberId);
        }

        return response;
    }

    public TeamDto getTeam(User user, GetTeamQueryValidator query) {
        String userId = user.getId();
        String teamId = query.getTeamId();
        String projectId = query.getProjectId();

        log.info("Getting team - userId: {}, teamId: {}, projectId: {}", userId, teamId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        return getTeamByIdWithMembers(teamId);
    }

    private TeamDto getTeamByIdWithMembers(String teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND.value()));

        TeamDto.TeamDtoBuilder teamDtoBuilder = TeamDto.builder()
                .id(team.getId())
                .name(team.getName())
                .memberCount(team.getMembers().size());

        if (team.getLeader() != null) {
            Member leader = memberRepository.findById(team.getLeader()).orElse(null);
            if (leader != null) {
                User leaderUser = userRepository.findById(leader.getUserId()).orElse(null);
                teamDtoBuilder.leader(TeamDto.LeaderDto.builder()
                        .id(leader.getId())
                        .email(leader.getEmail())
                        .role(leader.getRole().toString())
                        .user(leaderUser != null ? TeamDto.UserDto.builder()
                                .fullName(leaderUser.getFullName())
                                .avatar(leaderUser.getAvatar())
                                .build() : null)
                        .build());
            }
        }

        List<TeamDto.MemberDto> memberDtos = team.getMembers().stream()
                .map(memberId -> {
                    Member m = memberRepository.findById(memberId).orElse(null);
                    if (m == null) return null;
                    User u = userRepository.findById(m.getUserId()).orElse(null);
                    return TeamDto.MemberDto.builder()
                            .id(m.getId())
                            .email(m.getEmail())
                            .role(m.getRole().toString())
                            .user(u != null ? TeamDto.UserDto.builder()
                                    .fullName(u.getFullName())
                                    .avatar(u.getAvatar())
                                    .build() : null)
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        teamDtoBuilder.members(memberDtos);

        return teamDtoBuilder.build();
    }

    public Map<String, Object> addOrRemoveTeamLeader(User user, AddOrRemoveTeamLeaderValidator request) {
        String userId = user.getId();
        String teamId = request.getTeamId();
        String projectId = request.getProjectId();
        String memberId = request.getMemberId();
        Boolean isRemove = request.getIsRemove();

        log.info("Add/Remove team leader - userId: {}, teamId: {}, memberId: {}, isRemove: {}", 
                userId, teamId, memberId, isRemove);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ApiException("Team not found", HttpStatus.NOT_FOUND.value()));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        Member userMember = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("You have no permission to add member", HttpStatus.NOT_FOUND.value()));

        if (userMember.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to add member", HttpStatus.NOT_FOUND.value());
        }

        if (isRemove) {
            if (team.getLeader() == null || !team.getLeader().equals(memberId)) {
                throw new ApiException("Member is not the leader", HttpStatus.CONFLICT.value());
            }
            team.setLeader(null);
        } else {
            if (!team.getMembers().contains(memberId)) {
                throw new ApiException("Member is not in the team", HttpStatus.CONFLICT.value());
            }
            team.setLeader(memberId);
        }

        teamRepository.save(team);

        return Map.of(
                "teamId", teamId,
                "memberId", memberId,
                "isRemove", isRemove
        );
    }


}
