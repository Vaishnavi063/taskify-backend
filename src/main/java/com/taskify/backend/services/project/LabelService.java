package com.taskify.backend.services.project;

import com.taskify.backend.constants.MemberEnums.MemberRole;
import com.taskify.backend.models.auth.User;
import com.taskify.backend.models.project.Label;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.repository.project.LabelRepository;
import com.taskify.backend.repository.project.MemberRepository;
import com.taskify.backend.utils.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {
    private final LabelRepository labelRepository;
    private final MemberRepository memberRepository;

    public Map<String ,Object> create(User user, Label label) {
        log.info("Received request to create label for project {}",label);
        log.info("User {}",user);

        String userId = user.getId();
        String projectId = label.getProjectId().getId();

        Member member = memberRepository.findByUserIdAndProjectId(userId, projectId)
                .orElseThrow(() -> new ApiException("Member not found", HttpStatus.BAD_REQUEST.value()));

        if (member.getRole() == MemberRole.MEMBER) {
            throw new ApiException("You have no permission to create label", HttpStatus.FORBIDDEN.value());
        }

        Label createdLabel = labelRepository.save(label);
        return Map.of(
                "data", createdLabel
        );
    }
}
