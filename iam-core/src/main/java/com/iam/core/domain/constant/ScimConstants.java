package com.iam.core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * SCIM 2.0 standard schema URIs and constants.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScimConstants {
    public static final String URN_CORE_USER = "urn:ietf:params:scim:schemas:core:2.0:User";
    public static final String URN_ENTERPRISE_USER = "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";
}
