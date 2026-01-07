package com.iam.core.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.Map;

public record UserSyncEvent(@NotBlank(message = "traceId는 필수입니다")
                            String traceId,

                            @NotBlank(message = "systemId는 필수입니다")
                            String systemId,

                            @NotBlank(message = "eventType은 필수입니다")
                            @Pattern(regexp = "^(USER_CREATE|USER_UPDATE|USER_DELETE)$", message = "eventType은 USER_CREATE, USER_UPDATE, USER_DELETE 중 하나여야 합니다")
                            String eventType,

                            @NotNull(message = "timestamp는 필수입니다")
                            LocalDateTime timestamp,

                            @NotNull(message = "payload는 필수입니다")
                            Map<String, Object> payload) {
}
