package com.corems.common.security;

/**
 * Centralized roles enum for Core Microservices.
 * Use this enum across services instead of service-local role enums.
 */
public enum CoreMsRoles {
    SYSTEM,
    SUPER_ADMIN,
    USER_MS_ADMIN,
    USER_MS_USER
}
