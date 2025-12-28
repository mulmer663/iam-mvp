package com.iam.core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for standard attribute names used in payloads and mappings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttributeConstants {
    public static final String USERNAME = "userName";
    public static final String FAMILY_NAME = "familyName";
    public static final String GIVEN_NAME = "givenName";
    public static final String FORMATTED_NAME = "formattedName";
    public static final String TITLE = "title";
    public static final String ACTIVE = "active";
    public static final String EXTERNAL_ID = "externalId";

    // History specific keys
    public static final String SYNC_TYPE = "syncType";
    public static final String MAPPINGS = "mappings";
    public static final String SNAPSHOT = "snapshot";
    public static final String CHANGES = "changes";
    public static final String LAYER = "layer";
    public static final String DATA = "data";
    public static final String FIELD = "field";
    public static final String OLD_VALUE = "old";
    public static final String NEW_VALUE = "new";

    // Mapping keys
    public static final String FROM_LABEL = "fromLabel";
    public static final String TO_LABEL = "toLabel";
    public static final String FROM_FIELD = "fromField";
    public static final String TO_FIELD = "toField";
    public static final String VALUE = "value";

    // Common Extension keys
    public static final String EMP_NO = "empNo";
    public static final String DEPT_NAME = "deptName";
}
