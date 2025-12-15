package com.taskify.backend.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class DocumentEnums {

    public enum DocStatus {
        DRAFT("Draft"),
        PUBLISHED("Published"),
        ARCHIVED("Archived");
        
        private final String value;
        
        DocStatus(String value) {
            this.value = value;
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        @JsonCreator
        public static DocStatus fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (DocStatus status : DocStatus.values()) {
                if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown DocStatus: " + value + ". Valid values are: Draft, Published, Archived, DRAFT, PUBLISHED, ARCHIVED");
        }
    }

    public enum DocAccessType {
        PRIVATE("Private"),
        PUBLIC("Public"),
        TEAM("Team");
        
        private final String value;
        
        DocAccessType(String value) {
            this.value = value;
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        @JsonCreator
        public static DocAccessType fromString(String value) {
            for (DocAccessType type : DocAccessType.values()) {
                if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown DocAccessType: " + value);
        }
    }
    
    public enum DocType {
        DOCUMENT("Document"),
        WIKI("Wiki"),
        NOTE("Note");
        
        private final String value;
        
        DocType(String value) {
            this.value = value;
        }
        
        @JsonValue
        public String getValue() {
            return value;
        }
        
        @JsonCreator
        public static DocType fromString(String value) {
            for (DocType type : DocType.values()) {
                if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown DocType: " + value);
        }
    }
}
