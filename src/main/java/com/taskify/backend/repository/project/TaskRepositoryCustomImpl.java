package com.taskify.backend.repository.project;

import com.taskify.backend.models.project.Comment;
import com.taskify.backend.models.project.Member;
import com.taskify.backend.models.project.Task;
import com.taskify.backend.models.auth.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TaskRepositoryCustomImpl implements TaskRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final MemberRepository memberRepository;
    private final com.taskify.backend.repository.auth.UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Override
    public Optional<Map<String, Object>> getTaskWithComments(String taskId, String memberId) {
        // Fetch task using MongoTemplate
        Query query = new Query(Criteria.where("_id").is(new ObjectId(taskId)));
        Task task = mongoTemplate.findOne(query, Task.class);
        
        if (task == null || (task.getIsDeleted() != null && task.getIsDeleted())) {
            return Optional.empty();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("_id", task.getId());
        result.put("title", task.getTitle());
        result.put("description", task.getDescription());
        result.put("status", task.getStatus());
        result.put("priority", task.getPriority());
        result.put("dueDate", task.getDueDate());
        result.put("taskType", task.getTaskType());
        result.put("taskNumber", task.getTaskNumber());

        // Get creator
        String taskMemberId = task.getMemberId();
        log.info("Task memberId: {}", taskMemberId);
        log.info("Task assignees: {}", task.getAssignees());
        log.info("Task comments: {}", task.getComments());
        
        if (taskMemberId != null && !taskMemberId.isEmpty()) {
            Member creatorMember = memberRepository.findById(taskMemberId).orElse(null);
            log.info("Creator member found: {}", creatorMember != null);
            if (creatorMember != null) {
                String creatorUserId = creatorMember.getUserId();
                User creatorUser = creatorUserId != null ? userRepository.findById(creatorUserId).orElse(null) : null;
                Map<String, Object> creator = new HashMap<>();
                creator.put("fullName", creatorUser != null ? creatorUser.getFullName() : "");
                creator.put("email", creatorMember.getEmail());
                creator.put("role", creatorMember.getRole().toString());
                creator.put("avatar", creatorUser != null ? creatorUser.getAvatar() : null);
                result.put("creator", creator);
                result.put("isCreator", creatorMember.getId().equals(memberId));
            } else {
                result.put("creator", new HashMap<>());
                result.put("isCreator", false);
            }
        } else {
            result.put("creator", new HashMap<>());
            result.put("isCreator", false);
        }

        // Get members (assignees)
        List<Map<String, Object>> members = new ArrayList<>();
        if (task.getAssignees() != null && !task.getAssignees().isEmpty()) {
            for (String assigneeId : task.getAssignees()) {
                if (assigneeId != null && !assigneeId.isEmpty()) {
                    Member member = memberRepository.findById(assigneeId).orElse(null);
                    if (member != null) {
                        Map<String, Object> memberMap = new HashMap<>();
                        memberMap.put("_id", member.getId());
                        memberMap.put("email", member.getEmail());
                        members.add(memberMap);
                    }
                }
            }
        }
        result.put("members", members);
        result.put("isMember", task.getAssignees() != null && task.getAssignees().contains(memberId));

        // Get comments with authors
        List<Map<String, Object>> comments = new ArrayList<>();
        if (task.getComments() != null && !task.getComments().isEmpty()) {
            for (String commentId : task.getComments()) {
                if (commentId != null && !commentId.isEmpty()) {
                    Comment comment = commentRepository.findById(commentId).orElse(null);
                    if (comment != null) {
                        Map<String, Object> commentMap = new HashMap<>();
                        commentMap.put("_id", comment.getId());
                        commentMap.put("content", comment.getContent());
                        commentMap.put("commentType", comment.getCommentType().toString());
                        commentMap.put("createdAt", comment.getCreatedAt());
                        commentMap.put("updatedAt", comment.getUpdatedAt());

                        // Get comment author
                        String commentMemberId = comment.getMemberId();
                        if (commentMemberId != null && !commentMemberId.isEmpty()) {
                            Member authorMember = memberRepository.findById(commentMemberId).orElse(null);
                            if (authorMember != null) {
                                String authorUserId = authorMember.getUserId();
                                User authorUser = authorUserId != null ? userRepository.findById(authorUserId).orElse(null) : null;
                                
                                Map<String, Object> author = new HashMap<>();
                                author.put("_id", authorMember.getId());
                                author.put("email", authorMember.getEmail());
                                author.put("role", authorMember.getRole().toString());
                                author.put("isAuthor", authorMember.getId().equals(memberId));
                                
                                Map<String, Object> authorUserMap = new HashMap<>();
                                authorUserMap.put("fullName", authorUser != null ? authorUser.getFullName() : "");
                                authorUserMap.put("avatar", authorUser != null ? authorUser.getAvatar() : null);
                                author.put("user", authorUserMap);
                                
                                commentMap.put("author", author);
                            }
                        }
                        
                        comments.add(commentMap);
                    }
                }
            }
        }
        result.put("comments", comments);

        return Optional.of(result);
    }
}
