package com.corems.common.security;

import java.util.EnumSet;
import java.util.Set;

/**
 * Centralized roles enum for Core Microservices.
 * Use this enum across services instead of service-local role enums.
 */
public enum CoreMsRoles {
    // Interservice role
    SYSTEM,
    // Super Admin Role
    SUPER_ADMIN,
    // User Microservice Roles
    USER_MS_ADMIN,
    USER_MS_USER,
    // Communication Microservice Roles
    COMMUNICATION_MS_ADMIN,
    COMMUNICATION_MS_USER,
    // Translation Microservice Roles
    TRANSLATION_MS_ADMIN,
    // Document Microservice Roles
    DOCUMENT_MS_ADMIN,
    DOCUMENT_MS_USER;


    public static Set<CoreMsRoles> getSystemRoles() {
        return EnumSet.of(SYSTEM, SUPER_ADMIN);
    }
}
