package com.iam.core.domain.exception;

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

    // Validation 에러용
    private final List<FieldError> fieldErrors;

    // 추가 컨텍스트 정보
    private final Map<String, Object> metadata;

    @Builder
    public record FieldError(String field, Object rejectedValue, String message, String code) {
    }
}