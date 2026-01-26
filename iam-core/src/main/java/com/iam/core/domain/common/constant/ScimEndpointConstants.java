package com.iam.core.domain.common.constant;

import java.util.Set;

public class ScimEndpointConstants {
    public static final String USERS = "User";
    public static final String GROUPS = "Group";
    public static final String DEPARTMENTS = "Department";
    public static final String ROLES = "Role";
    public static final String SYSTEMS = "System";
    public static final String RESOURCES = "Resource";

    public static final Set<String> CORE_TYPES = Set.of(
            USERS, GROUPS, DEPARTMENTS, ROLES, SYSTEMS, RESOURCES);

    public static boolean isCoreType(String resourceType) {
        return CORE_TYPES.contains(resourceType);
    }
}
