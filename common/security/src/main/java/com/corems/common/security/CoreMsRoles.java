package com.corems.common.security;

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
    // Translation Microservice Roles
    TRANSLATION_MS_ADMIN
}
