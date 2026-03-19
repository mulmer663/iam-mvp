package com.iam.registry.domain.common.exception;

public class RuleExecutionException extends RuleEngineException {
    public RuleExecutionException(String detail, Throwable cause) {
        super(ErrorCode.TRANS_RULE_ERROR, detail, cause);
    }
}
