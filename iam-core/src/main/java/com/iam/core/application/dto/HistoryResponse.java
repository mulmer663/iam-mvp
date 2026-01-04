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
        java.util.Map<String, Object> resultData,
        java.util.Map<String, Object> requestPayload,
        Long parentHistoryId,
        Long userRevId,
        Long ruleRevId) {
}
