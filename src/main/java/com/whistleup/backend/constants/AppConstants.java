package com.whistleup.backend.constants;

public final class AppConstants {

    private AppConstants() {
    }

    public static final int MAX_EMERGENCY_CONTACTS = 4;
    public static final long MAX_TENANT_DOCUMENT_BYTES = 5L * 1024 * 1024;

    public static final String CONTACT_KIND_GENERAL = "GENERAL";
    public static final String CONTACT_KIND_EMERGENCY = "EMERGENCY";
}
