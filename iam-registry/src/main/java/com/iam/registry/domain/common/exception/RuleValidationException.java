package com.iam.registry.domain.common.exception;

public class RuleValidationException extends RuleEngineException {
    public RuleValidationException(String detail) {
        super(ErrorCode.TRANS_VALIDATION_ERROR, detail);
    }
}
