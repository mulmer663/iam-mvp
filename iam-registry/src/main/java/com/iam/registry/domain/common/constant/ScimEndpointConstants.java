package com.iam.registry.domain.common.constant;

import java.util.Set;

public class ScimEndpointConstants {
    public static final String USERS = "User";
    public static final String GROUPS = "Group";
    public static final String DEPARTMENTS = "Department";
    public static final String ROLES = "Role";
    public static final String SYSTEMS = "System";
    public static final String RESOURCES = "Resource";

    // Only types with dedicated static controllers (ScimUserController, etc.)
    public static final Set<String> CORE_TYPES = Set.of(USERS, GROUPS);

    public static boolean isCoreType(String resourceType) {
        return CORE_TYPES.contains(resourceType);
    }
}
