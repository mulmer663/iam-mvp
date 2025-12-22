package com.iam.core.dto;

import java.time.LocalDateTime;

public record UserSyncEvent(
        String traceId,
        String eventType,
        LocalDateTime timestamp,
        UserSyncPayload payload) {
}
