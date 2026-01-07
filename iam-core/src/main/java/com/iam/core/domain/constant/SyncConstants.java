package com.iam.core.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for synchronization types, event types, and statuses.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SyncConstants {
    // Event Types
    public static final String EVENT_USER_CREATE = "USER_CREATE";
    public static final String EVENT_USER_UPDATE_SIMPLE = "USER_UPDATE_SIMPLE";
    public static final String EVENT_USER_UPDATE_CRITICAL = "USER_UPDATE_CRITICAL";
    public static final String EVENT_USER_UPDATE = "USER_UPDATE";
    public static final String EVENT_USER_RETIRE = "USER_RETIRE";
    public static final String EVENT_SYNC_ERROR = "SYNC_ERROR";

    // Statuses
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";

    // MDC
    public static final String TRACE_ID = "traceId";
    public static final String OPERATION_TYPE = "operationType";

    // Directions
    public static final String DIRECTION_RECON = "RECON";
    public static final String DIRECTION_PROV = "PROV";
}
