package com.iam.core.application.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
                String id,
                String traceId,
                String type,
                String status,
                String targetUser,
                LocalDateTime createdAt,
                String message,
                String payload,
                Long parentHistoryId,
                Long durationMs) {
}
