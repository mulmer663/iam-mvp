package com.iam.core.domain.exception;

public class RuleValidationException extends RuleEngineException {
    public RuleValidationException(String detail) {
        super(ErrorCode.TRANS_VALIDATION_ERROR, detail);
    }
}
