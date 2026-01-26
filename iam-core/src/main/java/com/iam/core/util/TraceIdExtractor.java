package com.iam.core.util;

import com.iam.core.application.common.ProvisioningCommand;
import com.iam.core.application.common.UserSyncEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 다양한 소스에서 TraceId를 추출하는 유틸리티
 */
@Slf4j
@Component
public class TraceIdExtractor {

    /**
     * 우선순위에 따른 TraceId 추출
     * 1. Message Headers
     * 2. Payload 직접 접근
     * 3. JSON 파싱
     * 4. Reflection
     * 5. Fallback 생성
     */
    public String extract(org.springframework.messaging.Message<?> message) {
        String[] extractionMethods = {
                "headers", "payload", "json", "reflection"
        };

        for (String method : extractionMethods) {
            try {
                String traceId = switch (method) {
                    case "headers" -> extractFromHeaders(message);
                    case "payload" -> extractFromPayload(message.getPayload());
                    case "json" -> extractFromJsonString((String) message.getPayload());
                    case "reflection" -> extractUsingReflection(message.getPayload());
                    default -> null;
                };

                if (traceId != null && !traceId.trim().isEmpty()) {
                    log.debug("TraceId extracted using method: {}, value: {}", method, traceId);
                    return traceId;
                }
            } catch (Exception e) {
                log.debug("Failed to extract traceId using method: {}", method, e);
            }
        }

        String fallbackTraceId = generateFallbackTraceId();
        log.warn("Using fallback traceId: {}", fallbackTraceId);
        return fallbackTraceId;
    }

    /**
     * Message Headers에서 traceId 추출
     */
    private String extractFromHeaders(org.springframework.messaging.Message<?> message) {
        var headers = message.getHeaders();

        // Spring Cloud Sleuth/Micrometer tracing headers
        Object traceId = headers.get("X-Trace-Id");
        if (traceId != null) {
            return traceId.toString();
        }

        // Custom header
        traceId = headers.get("traceId");
        if (traceId != null) {
            return traceId.toString();
        }

        // AMQP native headers
        @SuppressWarnings("unchecked")
        Map<String, Object> amqpHeaders = (Map<String, Object>) headers.get("amqp_receivedMessageProperties");
        if (amqpHeaders != null) {
            Object amqpTraceId = amqpHeaders.get("traceId");
            if (amqpTraceId != null) {
                return amqpTraceId.toString();
            }
        }

        return null;
    }

    /**
     * Payload 객체에서 직접 traceId 추출
     */
    private String extractFromPayload(Object payload) {
        if (payload == null) {
            return null;
        }

        // UserSyncEvent인 경우 직접 접근
        if (payload instanceof UserSyncEvent event) {
            return event.traceId();
        }

        // ProvisioningCommand인 경우
        if (payload instanceof ProvisioningCommand command) {
            return command.traceId();
        }

        // JSON 문자열인 경우 파싱 시도
        if (payload instanceof String jsonString) {
            return extractFromJsonString(jsonString);
        }

        // Map 형태인 경우
        if (payload instanceof Map<?, ?> map) {
            Object traceId = map.get("traceId");
            return traceId != null ? traceId.toString() : null;
        }

        // Reflection을 사용한 일반적인 접근
        return extractUsingReflection(payload);
    }

    /**
     * JSON 문자열에서 traceId 추출
     */
    private String extractFromJsonString(String jsonString) {
        try {
            // Jackson ObjectMapper 사용
            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var jsonNode = objectMapper.readTree(jsonString);

            var traceIdNode = jsonNode.get("traceId");
            if (traceIdNode != null && !traceIdNode.isNull()) {
                return traceIdNode.asText();
            }
        } catch (Exception e) {
            log.debug("Failed to parse JSON for traceId extraction", e);
        }
        return null;
    }

    /**
     * Reflection을 사용한 traceId 추출
     */
    private String extractUsingReflection(Object payload) {
        try {
            Class<?> clazz = payload.getClass();

            // traceId 필드 직접 접근
            try {
                var field = clazz.getDeclaredField("traceId");
                field.setAccessible(true);
                Object value = field.get(payload);
                return value != null ? value.toString() : null;
            } catch (NoSuchFieldException ignored) {
                // 필드가 없으면 메서드 시도
            }

            // getTraceId() 메서드 호출 시도
            try {
                var method = clazz.getMethod("traceId"); // record accessor
                Object value = method.invoke(payload);
                return value != null ? value.toString() : null;
            } catch (NoSuchMethodException ignored) {
                // ignored
            }

            // getTraceId() getter 메서드 시도
            try {
                var method = clazz.getMethod("getTraceId");
                Object value = method.invoke(payload);
                return value != null ? value.toString() : null;
            } catch (NoSuchMethodException ignored) {
                // ignored
            }

        } catch (Exception e) {
            log.debug("Failed to extract traceId using reflection", e);
        }

        return null;
    }

    /**
     * Fallback traceId 생성
     */
    private String generateFallbackTraceId() {
        return "unknown-" + System.currentTimeMillis() + "-" +
                Integer.toHexString(Thread.currentThread().hashCode());
    }
}