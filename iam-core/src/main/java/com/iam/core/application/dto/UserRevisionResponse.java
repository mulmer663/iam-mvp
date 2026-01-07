package com.iam.core.application.dto;

import java.time.LocalDateTime;

public record UserRevisionResponse(
        int revId,
        String traceId,
        String operatorId,
        String operationType,
        LocalDateTime timestamp,
        ScimUserResponse profile) {
}
