package com.iam.core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for synchronization types, event types, and statuses.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SyncConstants {
    // Sync Types
    public static final String TYPE_JOIN = "JOIN";
    public static final String TYPE_UPDATE_SIMPLE = "UPDATE_SIMPLE";
    public static final String TYPE_UPDATE_CRITICAL = "UPDATE_CRITICAL";

    // Event Types
    public static final String EVENT_USER_CREATE = "USER_CREATE";
    public static final String EVENT_USER_UPDATE = "USER_UPDATE";
    public static final String EVENT_SYNC_ERROR = "SYNC_ERROR";
    public static final String EVENT_HR_SYNC = "HR_SYNC";
    public static final String EVENT_AD_PROVISION = "AD_PROVISION";

    // Statuses
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
}
