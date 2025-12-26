package com.iam.core.application.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
        String id,
        String traceId,
        String type,
        String status,
        String target,
        LocalDateTime time,
        String message,
        String payload // JSON string
) {
}
