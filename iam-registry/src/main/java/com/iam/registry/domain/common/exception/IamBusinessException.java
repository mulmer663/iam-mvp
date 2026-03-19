package com.iam.registry.domain.common.exception;

public class IamBusinessException extends BaseIamException {

    public IamBusinessException(ErrorCode errorCode, String traceId, String message, Object... messageArgs) {
        super(errorCode, traceId, message, messageArgs);
    }

    public IamBusinessException(ErrorCode errorCode, String traceId, String message, Throwable cause,
            Object... messageArgs) {
        super(errorCode, traceId, message, cause, messageArgs);
    }
}
