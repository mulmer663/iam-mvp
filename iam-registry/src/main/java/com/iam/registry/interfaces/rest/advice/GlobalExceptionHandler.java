package com.iam.registry.interfaces.rest.advice;

import com.iam.registry.domain.common.exception.BaseIamException;
import com.iam.registry.domain.common.exception.ErrorCode;
import com.iam.registry.domain.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SCIM_ERROR_SCHEMA = "urn:ietf:params:scim:api:messages:2.0:Error";
    private static final String SCIM_PATH_PREFIX = "/scim/v2/";

    // ── RFC 7644 §3.12 SCIM 에러 응답 ────────────────────────────────────────

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScimErrorResponse(
            List<String> schemas,
            String scimType,
            String status,
            String detail
    ) {
        static ScimErrorResponse of(String scimType, HttpStatus httpStatus, String detail) {
            return new ScimErrorResponse(
                    List.of(SCIM_ERROR_SCHEMA),
                    scimType,
                    String.valueOf(httpStatus.value()),
                    detail
            );
        }
    }

    // ── scimType 매핑 (RFC 7644 Table 9) ─────────────────────────────────────

    private String toScimType(ErrorCode code) {
        return switch (code) {
            case INVALID_SCIM_FILTER                    -> "invalidFilter";
            case USER_ALREADY_EXISTS,
                 EXTERNAL_ID_ALREADY_EXISTS             -> "uniqueness";
            case MISSING_REQUIRED_FIELD,
                 VALIDATION_FAILED,
                 INVALID_REQUEST_FORMAT                 -> "invalidValue";
            // RFC 7644 §3.12: noTarget은 PATCH path 해석 실패 전용 — 일반 404에는 생략
            case USER_NOT_FOUND,
                 RESOURCE_NOT_FOUND                    -> null;
            default                                    -> null;
        };
    }

    // ── 경로 판별 ─────────────────────────────────────────────────────────────

    private boolean isScimRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(SCIM_PATH_PREFIX);
    }

    // ── traceId 결정 ──────────────────────────────────────────────────────────

    private String resolveTraceId(String exceptionTraceId) {
        String mdc = MDC.get("traceId");
        if (mdc != null && !mdc.isBlank()) return mdc;
        if (exceptionTraceId != null && !exceptionTraceId.isBlank()) return exceptionTraceId;
        return UUID.randomUUID().toString();
    }

    // ── BaseIamException (IamBusinessException 포함) ─────────────────────────

    @ExceptionHandler(BaseIamException.class)
    public ResponseEntity<?> handleIamException(BaseIamException ex, HttpServletRequest request) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = code.getHttpStatus();
        String traceId = resolveTraceId(ex.getTraceId());

        if (status.is4xxClientError()) {
            log.warn("[{}] {} — {} | path={}", traceId, code.getCode(), ex.getMessage(), request.getRequestURI());
        } else {
            log.error("[{}] {} — {} | path={}", traceId, code.getCode(), ex.getMessage(), request.getRequestURI(), ex);
        }

        if (isScimRequest(request)) {
            ScimErrorResponse body = ScimErrorResponse.of(toScimType(code), status, ex.getMessage());
            return ResponseEntity.status(status).body(body);
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(code.getCode())
                .message(code.getMessage())
                .detail(ex.getMessage())
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(status.value())
                .build();
        return ResponseEntity.status(status).body(body);
    }

    // ── @Valid 검증 실패 ──────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex,
                                              HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        log.warn("[{}] Validation failed | path={}", traceId, request.getRequestURI());

        if (isScimRequest(request)) {
            String detail = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .findFirst()
                    .orElse("입력 값 검증에 실패했습니다");
            return ResponseEntity.badRequest()
                    .body(ScimErrorResponse.of("invalidValue", HttpStatus.BAD_REQUEST, detail));
        }

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue())
                        .message(fe.getDefaultMessage())
                        .code(fe.getCode())
                        .build())
                .toList();

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ErrorCode.VALIDATION_FAILED.getMessage())
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    // ── 파라미터 타입 불일치 ──────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        String detail = "파라미터 '%s'의 값 '%s'이 올바르지 않습니다".formatted(ex.getName(), ex.getValue());
        log.warn("[{}] Type mismatch — {} | path={}", traceId, detail, request.getRequestURI());

        if (isScimRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(ScimErrorResponse.of("invalidValue", HttpStatus.BAD_REQUEST, detail));
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
                .message(ErrorCode.INVALID_REQUEST_FORMAT.getMessage())
                .detail(detail)
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    // ── JSON 파싱 실패 ────────────────────────────────────────────────────────

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleNotReadable(HttpMessageNotReadableException ex,
                                               HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        log.warn("[{}] Message not readable | path={}", traceId, request.getRequestURI());

        if (isScimRequest(request)) {
            return ResponseEntity.badRequest()
                    .body(ScimErrorResponse.of("invalidSyntax", HttpStatus.BAD_REQUEST, "요청 본문을 읽을 수 없습니다"));
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
                .message(ErrorCode.INVALID_REQUEST_FORMAT.getMessage())
                .detail("요청 본문을 읽을 수 없습니다")
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    // ── 405 Method Not Allowed ────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                    HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        String detail = "허용되지 않는 HTTP 메서드입니다: " + ex.getMethod();
        log.warn("[{}] Method not allowed — {} | path={}", traceId, ex.getMethod(), request.getRequestURI());

        if (isScimRequest(request)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .body(ScimErrorResponse.of(null, HttpStatus.METHOD_NOT_ALLOWED, detail));
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.INVALID_REQUEST_FORMAT.getCode())
                .message(detail)
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    // ── 404 Not Found ─────────────────────────────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNotFound(NoResourceFoundException ex,
                                            HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        String detail = "요청한 리소스를 찾을 수 없습니다: " + request.getRequestURI();
        log.warn("[{}] Resource not found | path={}", traceId, request.getRequestURI());

        if (isScimRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ScimErrorResponse.of("noTarget", HttpStatus.NOT_FOUND, detail));
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message(ErrorCode.RESOURCE_NOT_FOUND.getMessage())
                .detail(detail)
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.NOT_FOUND.value())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── Fallback — 5xx ───────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnexpected(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(null);
        log.error("[{}] Unexpected error | path={}", traceId, request.getRequestURI(), ex);

        if (isScimRequest(request)) {
            // 5xx: 내부 오류 상세 노출 금지
            return ResponseEntity.internalServerError()
                    .body(ScimErrorResponse.of(null, HttpStatus.INTERNAL_SERVER_ERROR,
                            "내부 서버 오류가 발생했습니다. traceId=" + traceId));
        }

        ErrorResponse body = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .traceId(traceId)
                .path(request.getRequestURI())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
        return ResponseEntity.internalServerError().body(body);
    }
}
