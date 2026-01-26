package com.iam.core.application.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 외부 시스템으로부터 유입되는 사용자 동기화 이벤트 전문.
 * Jackson을 통해 JSONB 컬럼에 저장될 때 timestamp 형식을 유지하기 위해 @JsonFormat을 사용합니다.
 */
public record UserSyncEvent(
        @NotBlank(message = "traceId는 필수입니다") String traceId,

        @NotBlank(message = "systemId는 필수입니다") String systemId,

        @NotBlank(message = "eventType은 필수입니다") @Pattern(regexp = "^(USER_CREATE|USER_UPDATE|USER_UPDATE_SIMPLE|USER_UPDATE_CRITICAL|USER_RETIRE)$", message = "eventType 형식이 올바르지 않습니다") String eventType,

        @NotNull(message = "timestamp는 필수입니다") @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime timestamp,

        @NotNull(message = "payload는 필수입니다") Map<String, Object> payload) {
}
