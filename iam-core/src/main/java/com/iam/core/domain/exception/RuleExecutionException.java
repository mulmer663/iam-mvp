package com.iam.core.domain.exception;

public class RuleExecutionException extends RuleEngineException {
    public RuleExecutionException(String detail, Throwable cause) {
        super(ErrorCode.TRANS_RULE_ERROR, detail, cause);
    }
}
