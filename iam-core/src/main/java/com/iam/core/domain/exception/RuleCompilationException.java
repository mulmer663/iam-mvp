package com.iam.core.domain.exception;

public class RuleCompilationException extends RuleEngineException {
    public RuleCompilationException(String detail, Throwable cause) {
        super(ErrorCode.TRANS_COMPILATION_ERROR, detail, cause);
    }
}
