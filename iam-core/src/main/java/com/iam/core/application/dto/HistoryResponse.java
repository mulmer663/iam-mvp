package com.iam.core.application.dto;

import java.time.LocalDateTime;

public record HistoryResponse(
                String id,
                String traceId,
                String type,
                String status,
                String target,
                String sourceSystem,
                String targetSystem,
                LocalDateTime time,
                String message,
                String payload,
                String requestPayload,
                Long parentHistoryId,
                Long durationMs) {
}
