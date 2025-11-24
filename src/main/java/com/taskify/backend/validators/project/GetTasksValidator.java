package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;

public class GetTasksValidator {

    @NotEmpty(message = "ProjectId is required")
    private String projectId = "";

    private String title = "";
    private String priority = "";
    private String status = "";
    private boolean sortByCreated = false;
    private boolean assignedToMe = false;
    private boolean createdByMe = false;
    private int page = 1;
    private int limit = 6;

    // ======= Getters =======
    public String getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public String getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public boolean getSortByCreated() {
        return sortByCreated;
    }

    public boolean getAssignedToMe() {
        return assignedToMe;
    }

    public boolean getCreatedByMe() {
        return createdByMe;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    // ======= Setters =======
    public void setProjectId(String projectId) {
        this.projectId = (projectId == null) ? "" : projectId.trim();
    }

    public void setTitle(String title) {
        this.title = (title == null || title.trim().isEmpty()) ? "" : title.trim();
    }

    public void setPriority(String priority) {
        if (priority == null || priority.trim().isEmpty() || priority.equalsIgnoreCase("All")) {
            this.priority = "";
        } else {
            this.priority = priority.trim();
        }
    }

    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty() || status.equalsIgnoreCase("All")) {
            this.status = "";
        } else {
            this.status = status.trim();
        }
    }

    public void setSortByCreated(String sortByCreated) {
        this.sortByCreated = sortByCreated != null &&
                !sortByCreated.equalsIgnoreCase("false") &&
                !sortByCreated.isEmpty();
    }

    public void setAssignedToMe(String assignedToMe) {
        this.assignedToMe = assignedToMe != null &&
                !assignedToMe.equalsIgnoreCase("false") &&
                !assignedToMe.isEmpty();
    }

    public void setCreatedByMe(String createdByMe) {
        this.createdByMe = createdByMe != null &&
                !createdByMe.equalsIgnoreCase("false") &&
                !createdByMe.isEmpty();
    }

    public void setPage(String page) {
        try {
            int parsedPage = Integer.parseInt(page);
            this.page = parsedPage > 0 ? parsedPage : 1;
        } catch (NumberFormatException e) {
            this.page = 1;
        }
    }

    public void setLimit(String limit) {
        try {
            int parsedLimit = Integer.parseInt(limit);
            this.limit = (parsedLimit > 0 && parsedLimit <= 100) ? parsedLimit : 6;
        } catch (NumberFormatException e) {
            this.limit = 6;
        }
    }
}
