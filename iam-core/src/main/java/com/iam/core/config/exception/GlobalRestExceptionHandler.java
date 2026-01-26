package com.iam.core.config.exception;

import com.iam.core.domain.common.exception.BaseIamException;
import com.iam.core.domain.common.exception.ErrorCode;
import com.iam.core.domain.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    @Value("${app.error.include-stack-trace:false}")
    private boolean includeStackTrace;

    @ExceptionHandler(BaseIamException.class)
    public ResponseEntity<ErrorResponse> handleIamException(BaseIamException ex) {
        ErrorCode errorCode = ex.getErrorCode();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(ex.getMessage())
                .traceId(ex.getTraceId())
                .path(getCurrentPath())
                .status(errorCode.getHttpStatus().value())
                .build();

        logError(ex, errorResponse);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String traceId = generateTraceId();

        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .code(error.getCode())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.VALIDATION_FAILED.getCode())
                .message(ErrorCode.VALIDATION_FAILED.getMessage())
                .detail("입력 값 검증 실패: " + fieldErrors.size() + "개 필드")
                .traceId(traceId)
                .path(getCurrentPath())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();

        log.warn("Validation failed - traceId: {}, errors: {}", traceId, fieldErrors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        String traceId = generateTraceId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.DATABASE_ERROR.getCode())
                .message(ErrorCode.DATABASE_ERROR.getMessage())
                .detail(includeStackTrace ? ex.getMessage() : "데이터베이스 연결 오류")
                .traceId(traceId)
                .path(getCurrentPath())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        log.error("Database error - traceId: {}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        String traceId = generateTraceId();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .detail(includeStackTrace ? ex.getMessage() : "예상치 못한 오류가 발생했습니다")
                .traceId(traceId)
                .path(getCurrentPath())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        log.error("Unexpected error - traceId: {}", traceId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private void logError(BaseIamException ex, ErrorResponse errorResponse) {
        if (ex.getErrorCode().getHttpStatus().is5xxServerError()) {
            log.error("Server error - traceId: {}, errorCode: {}",
                    ex.getTraceId(), ex.getErrorCode().getCode(), ex);
        } else {
            log.warn("Client error - traceId: {}, errorCode: {}, message: {}",
                    ex.getTraceId(), ex.getErrorCode().getCode(), ex.getMessage());
        }
    }

    private String getCurrentPath() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
            return request.getRequestURI();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}