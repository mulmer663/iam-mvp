package com.iam.core.domain.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // === Validation Errors (4000~4099) ===
    VALIDATION_FAILED("IAM-4000", "입력 값 검증에 실패했습니다", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_FORMAT("IAM-4001", "요청 형식이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    MISSING_REQUIRED_FIELD("IAM-4002", "필수 필드가 누락되었습니다", HttpStatus.BAD_REQUEST),

    // === Business Logic Errors (4100~4199) ===
    USER_NOT_FOUND("IAM-4100", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("IAM-4101", "이미 존재하는 사용자입니다", HttpStatus.CONFLICT),
    EXTERNAL_ID_ALREADY_EXISTS("IAM-4102", "이미 연동된 외부 시스템 식별자입니다", HttpStatus.CONFLICT),
    RESOURCE_NOT_FOUND("IAM-4103", "찾을 수 없는 리소스 입니다", HttpStatus.NOT_FOUND),

    // === Authentication & Authorization (4200~4299) ===
    UNAUTHORIZED("IAM-4200", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("IAM-4201", "접근 권한이 없습니다", HttpStatus.FORBIDDEN),
    TOKEN_EXPIRED("IAM-4202", "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),

    // === External System Errors (4300~4399) ===
    EXTERNAL_SYSTEM_ERROR("IAM-4300", "외부 시스템 연동 오류", HttpStatus.BAD_GATEWAY),
    HR_SYSTEM_UNAVAILABLE("IAM-4301", "HR 시스템을 사용할 수 없습니다", HttpStatus.SERVICE_UNAVAILABLE),
    AD_SYSTEM_ERROR("IAM-4302", "Active Directory 연동 오류", HttpStatus.BAD_GATEWAY),

    // === Rule Engine Errors (4400~4499) ===
    TRANS_RULE_ERROR("IAM-4400", "변환 규칙 실행 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANS_COMPILATION_ERROR("IAM-4401", "변환 규칙 스크립트 컴파일에 실패했습니다 (문법 오류)", HttpStatus.BAD_REQUEST),
    TRANS_VALIDATION_ERROR("IAM-4402", "데이터 변환 검증에 실패했습니다", HttpStatus.BAD_REQUEST),

    // === Internal Server Errors (5000~5099) ===
    INTERNAL_SERVER_ERROR("IAM-5000", "내부 서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("IAM-5001", "데이터베이스 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_PROCESSING_ERROR("IAM-5002", "메시지 처리 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_PERSISTENCE_ERROR("IAM-5003", "사용자 정보 저장 또는 수정 중 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}