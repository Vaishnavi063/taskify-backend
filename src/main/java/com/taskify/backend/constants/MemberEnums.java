package com.taskify.backend.constants;

import com.fasterxml.jackson.annotation.JsonCreator;

public class MemberEnums {

    public enum MemberRole {
        OWNER,
        ADMIN,
        MEMBER
    }

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    @JsonCreator
    public static InvitationStatus fromString(String key) {
        return key == null ? null : InvitationStatus.valueOf(key.toUpperCase());
    }
}

