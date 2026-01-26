package com.iam.core.application.common;

import java.time.LocalDateTime;

public record HistoryResponse(
                String id,
                String traceId,
                String eventType,
                String status,
                String target,
                String sourceSystem,
                String targetSystem,
                String syncDirection,
                LocalDateTime time,
                String message,
                java.util.Map<String, Object> resultData,
                java.util.Map<String, Object> requestPayload,
                Long parentHistoryId,
                Long userRevId,
                Long ruleRevId) {
}
