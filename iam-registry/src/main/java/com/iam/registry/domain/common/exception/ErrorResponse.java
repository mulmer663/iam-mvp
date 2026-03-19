package com.iam.registry.domain.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String errorCode;
    private final String message;
    private final String detail;
    private final String traceId;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    private final String path;
    private final Integer status;

    // Validation ?�러??
    private final List<FieldError> fieldErrors;

    // 추�? 컨텍?�트 ?�보
    private final Map<String, Object> metadata;

    @Builder
    public record FieldError(String field, Object rejectedValue, String message, String code) {
    }
}
