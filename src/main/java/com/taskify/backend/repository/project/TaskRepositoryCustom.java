package com.taskify.backend.repository.project;

import java.util.Map;
import java.util.Optional;

public interface TaskRepositoryCustom {
    Optional<Map<String, Object>> getTaskWithComments(String taskId, String memberId);
}
