package com.whistleup.backend.constants;

public enum IssueType {
    INFO("INFO"),
    ALERT("ALERT"),
    SUCCESS("SUCCESS");
    final String type;
    IssueType(String type) {
        this.type = type;
    }
}
