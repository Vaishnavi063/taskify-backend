package com.taskify.backend.validators.project;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetMembersQuery {
    private String projectId;
    private String email = "";
    private String invitationStatus = "";
    private int page = 1;
    private int limit = 10;

    public void sanitize() {
        if (invitationStatus != null && invitationStatus.equalsIgnoreCase("All")) {
            invitationStatus = "";
        }
        if (email == null) {
            email = "";
        }
        if (page <= 0) {
            page = 1;
        }
        if (limit <= 0) {
            limit = 10;
        }
    }
}

