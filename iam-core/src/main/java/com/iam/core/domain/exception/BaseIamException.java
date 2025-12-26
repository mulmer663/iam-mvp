package com.iam.core.domain.exception;

import lombok.Getter;

@Getter
public abstract class BaseIamException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String traceId;
    private final Object[] messageArgs;

    protected BaseIamException(ErrorCode errorCode, String traceId, String message, Object... messageArgs) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = traceId;
        this.messageArgs = messageArgs;
    }

    protected BaseIamException(ErrorCode errorCode, String traceId, String message, Throwable cause,
            Object... messageArgs) {
        super(message, cause);
        this.errorCode = errorCode;
        this.traceId = traceId;
        this.messageArgs = messageArgs;
    }
}