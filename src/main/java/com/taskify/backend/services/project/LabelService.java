package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.dto.Label.GetLabelsResponseDto;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Label;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.repository.project.LabelRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.utils.ApiException;
import com.taskify.backend.validators.project.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    public Map<String, Object> createLabel(User user, LabelValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();

        log.info("Creating label - userId: {}, projectId: {}", userId, projectId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to create label", HttpStatus.FORBIDDEN.value());
        }

        Label label = Label.builder()
                .projectId(projectId)
                .name(request.getName())
                .description(request.getDescription())
                .color(request.getColor())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        label = labelRepository.save(label);

        return convertLabelToMap(label);
    }

    public Map<String, Object> updateLabel(User user, UpdateLabelValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String labelId = request.getLableId();

        log.info("Updating label - userId: {}, labelId: {}", userId, labelId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to update label", HttpStatus.FORBIDDEN.value());
        }

        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ApiException("Label not found", HttpStatus.NOT_FOUND.value()));

        label.setName(request.getName());
        label.setDescription(request.getDescription());
        label.setColor(request.getColor());
        label.setUpdatedAt(Instant.now());

        label = labelRepository.save(label);

        return convertLabelToMap(label);
    }

    public Map<String, Object> deleteLabel(User user, DeleteLabelValidator request) {
        String userId = user.getId();
        String projectId = request.getProjectId();
        String labelId = request.getLableId();

        log.info("Deleting label - userId: {}, labelId: {}", userId, labelId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to delete label", HttpStatus.FORBIDDEN.value());
        }

        labelRepository.deleteById(labelId);

        return Map.of("lableId", labelId);
    }

    public GetLabelsResponseDto getLabels(User user, GetLabelsQueryValidator query) {
        String userId = user.getId();
        String projectId = query.getProjectId();
        String name = query.getName() != null ? query.getName() : "";
        int page = Math.max(0, query.getPage() - 1);
        int limit = query.getLimit();

        log.info("Getting labels - userId: {}, projectId: {}", userId, projectId);

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.NOT_FOUND.value()));

        Pattern searchPattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);

        List<Label> allLabels = labelRepository.findAll().stream()
                .filter(label -> label.getProjectId() != null && label.getProjectId().equals(projectId))
                .filter(label -> name.isEmpty() || (label.getName() != null && searchPattern.matcher(label.getName()).find()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        int total = allLabels.size();
        int totalPages = (int) Math.ceil((double) total / limit);
        int start = page * limit;
        int end = Math.min(start + limit, total);
        List<Label> paginatedLabels = start < total ? allLabels.subList(start, end) : List.of();

        List<GetLabelsResponseDto.LabelDto> labelDtos = paginatedLabels.stream()
                .map(label -> GetLabelsResponseDto.LabelDto.builder()
                        .id(label.getId())
                        .projectId(label.getProjectId())
                        .name(label.getName())
                        .description(label.getDescription())
                        .color(label.getColor())
                        .createdAt(label.getCreatedAt())
                        .updatedAt(label.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return GetLabelsResponseDto.builder()
                .lables(labelDtos)
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

    private Map<String, Object> convertLabelToMap(Label label) {
        Map<String, Object> result = new HashMap<>();
        result.put("_id", label.getId());
        result.put("projectId", label.getProjectId());
        result.put("name", label.getName());
        result.put("description", label.getDescription());
        result.put("color", label.getColor());
        result.put("createdAt", label.getCreatedAt());
        result.put("updatedAt", label.getUpdatedAt());
        return result;
    }
}
