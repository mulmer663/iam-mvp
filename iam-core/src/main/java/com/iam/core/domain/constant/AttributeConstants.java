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
}
