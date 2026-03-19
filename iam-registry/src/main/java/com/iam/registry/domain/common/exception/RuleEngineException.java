package com.iam.registry.domain.common.exception;

import lombok.Getter;

@Getter
public abstract class RuleEngineException extends BaseIamException {
    public RuleEngineException(ErrorCode errorCode, String detail) {
        super(errorCode, null, detail);
    }

    public RuleEngineException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, null, detail, cause);
    }
}
