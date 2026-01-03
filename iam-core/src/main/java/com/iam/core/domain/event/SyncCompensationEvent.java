package com.iam.core.domain.event;

import java.time.LocalDateTime;

/**
 * Event triggered when a synchronization operation fails and requires
 * compensation
 * (e.g., rolling back a snapshot in the source connector).
 */
public record SyncCompensationEvent(
        String traceId,
        String systemId,
        String externalId,
        String reason,
        LocalDateTime timestamp) {
}
