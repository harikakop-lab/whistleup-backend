package com.whistleup.backend.constants;

public enum Roles {
    SYSTEM_ADMIN,
    ADMIN,
    USER,
    OWNER,
    VISITOR,
    /** Home / on-site service staff profile (stored in `profile.role`). */
    SERVICE_PERSON
}
